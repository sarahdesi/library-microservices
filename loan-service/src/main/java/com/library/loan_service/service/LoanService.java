package com.library.loan_service.service;


import com.library.loan_service.client.BookClient;
import com.library.loan_service.client.UserClient;
import com.library.loan_service.dto.*;
import com.library.loan_service.exception.BusinessException;
import com.library.loan_service.exception.ResourceNotFoundException;
import com.library.loan_service.model.Fine;
import com.library.loan_service.model.Loan;
import com.library.loan_service.model.LoanPolicy;
import com.library.loan_service.model.LoanStatus;
import com.library.loan_service.repository.FineRepository;
import com.library.loan_service.repository.LoanRepository;
import com.library.loan_service.repository.UserLoanLockRepository;
import com.library.loan_service.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {

    private static final BigDecimal FINE_PER_DAY = new BigDecimal("2.00");

    private final LoanRepository loanRepository;
    private final FineRepository fineRepository;
    private final UserLoanLockRepository userLoanLockRepository;
    private final UserClient userClient;
    private final BookClient bookClient;
    private final TransactionTemplate transactionTemplate;

    public LoanResponse create(LoanRequest request) {
        UserData user = userClient.getUser(request.userId());
        // The librarian is a user without type: she runs the counter, she does not borrow
        if (user.type() == null) {
            throw new BusinessException("Only members can borrow books");
        }
        LoanPolicy policy = LoanPolicy.fromUserType(user.type());

        userLoanLockRepository.ensureExists(user.id());
        AtomicBoolean copyReserved = new AtomicBoolean(false);
        try {
            return transactionTemplate.execute(status -> {
                // Loans of the same user wait here, one at a time, until the commit
                userLoanLockRepository.lockByUserId(user.id());

                //RN05: usuario com multa pendente ou livro atrasado nao pode pegar outro livro
                PendingResponse pending = getPending(user.id());
                if (pending.hasPendingFines() || pending.hasOverdueLoans()) {
                    throw new BusinessException("User has pending fines or overdue loans");
                }

                //RN03: limite de livros ao mesmo tempo, por tipo de usuario
                long activeLoans = loanRepository.countByUserIdAndStatus(user.id(), LoanStatus.ACTIVE);
                if (activeLoans >= policy.getMaxBooks()) {
                    throw new BusinessException("Loan limit reached (" + policy.getMaxBooks() + " books)");
                }

                //RN09: nao pode pegar dois exemplares do mesmo livro
                if (loanRepository.existsByUserIdAndBookIdAndStatus(user.id(), request.bookId(), LoanStatus.ACTIVE)) {
                    throw new BusinessException("User already has a copy of this book");
                }

                //RN01 + RN02: reserva atomica no catalogo ("No copies available" se nao houver exemplar),
                //feita antes de gravar o emprestimo
                bookClient.changeAvailability(request.bookId(), -1);
                copyReserved.set(true);

                Loan loan = new Loan();
                loan.setUserId(user.id());
                loan.setBookId(request.bookId());
                loan.setLoanDate(LocalDate.now());
                loan.setDueDate(LocalDate.now().plusDays(policy.getLoanDays()));
                loan.setRenewalCount(0);
                loan.setStatus(LoanStatus.ACTIVE);
                return LoanResponse.from(loanRepository.saveAndFlush(loan));
            });
        } catch (RuntimeException e) {
            // the loan was not saved (or the commit failed): give the reserved copy back
            if (copyReserved.get()) {
                compensate(request.bookId(), 1, e);
            }
            throw e;
        }
    }

    public LoanResponse registerReturn(Long loanId) {
        AtomicReference<String> copyReleased = new AtomicReference<>();
        try {
            return transactionTemplate.execute(status -> {
                // A concurrent return of the same loan waits here and then sees RETURNED
                Loan loan = getLoanForUpdate(loanId);
                if (loan.getStatus() == LoanStatus.RETURNED) {
                    throw new BusinessException("Loan already returned");
                }

                LocalDate today = LocalDate.now();
                loan.setReturnDate(today);
                loan.setStatus(LoanStatus.RETURNED);
                Loan saved = loanRepository.saveAndFlush(loan);

                //RN04: devolução atrasada gera multa por dia de atraso
                if (today.isAfter(loan.getDueDate())) {
                    long daysLate = ChronoUnit.DAYS.between(loan.getDueDate(), today);
                    Fine fine = new Fine();
                    fine.setLoanId(loan.getId());
                    fine.setUserId(loan.getUserId());
                    fine.setDaysLate((int) daysLate);
                    fine.setAmount(FINE_PER_DAY.multiply(BigDecimal.valueOf(daysLate)));
                    fine.setPaid(false);
                    fineRepository.save(fine);
                }

                //RN02: a devolução aumenta 1 exemplar disponivel (so quem venceu chega aqui).
                //Se falhar, a transacao desfaz a devolucao e a multa.
                bookClient.changeAvailability(loan.getBookId(), 1);
                copyReleased.set(loan.getBookId());
                return LoanResponse.from(saved);
            });
        } catch (RuntimeException e) {
            // the copy was given back but the commit failed: take it again
            if (copyReleased.get() != null) {
                compensate(copyReleased.get(), -1, e);
            }
            throw e;
        }
    }

    public LoanResponse renew(Long loanId, CurrentUser currentUser) {
        return transactionTemplate.execute(status -> {
            // Waits for a concurrent return/renewal, so it never works on a stale loan
            Loan loan = getLoanForUpdate(loanId);
            // A member renews only their own loans; the librarian renews any
            currentUser.requireAccessTo(loan.getUserId());
            if (loan.getStatus() == LoanStatus.RETURNED) {
                throw new BusinessException("Returned loans cannot be renewed");
            }

            //RN06: emprestimo atrasado nao pode ser renovado
            if (loan.isOverdue()) {
                throw new BusinessException("Overdue loans cannot be renewed");
            }

            UserData user = userClient.getUser(loan.getUserId());
            LoanPolicy policy = LoanPolicy.fromUserType(user.type());
            if (loan.getRenewalCount() >= policy.getMaxRenewals()) {
                throw new BusinessException("Renewal limit reached (" + policy.getMaxRenewals() + ")");
            }

            //RN07: a renovação soma um novo prazo ao emprestimo
            loan.setDueDate(loan.getDueDate().plusDays(policy.getLoanDays()));
            loan.setRenewalCount(loan.getRenewalCount() + 1);
            return LoanResponse.from(loanRepository.saveAndFlush(loan));
        });
    }

        public LoanResponse findById(Long loanId){
            return LoanResponse.from(getLoan(loanId));
        }

        public List<LoanResponse> findByUser(Long userId){
            return loanRepository.findByUserIdOrderByLoanDateDesc(userId).stream()
                    .map(LoanResponse::from)
                    .toList();
        }
    public PendingResponse getPending(Long userId) {
        boolean hasPendingFines = fineRepository.existsByUserIdAndPaidFalse(userId);
        boolean hasOverdueLoans = loanRepository.existsByUserIdAndStatusAndDueDateBefore(
                userId, LoanStatus.ACTIVE, LocalDate.now());
        return new PendingResponse(hasPendingFines, hasOverdueLoans);
    }

    private Loan getLoanForUpdate(Long loanId) {
        return loanRepository.findByIdForUpdate(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
    }

    // Best effort: if the compensation also fails, the availability stays wrong and is logged
    private void compensate(String bookId, int change, RuntimeException cause) {
        try {
            bookClient.changeAvailability(bookId, change);
        } catch (RuntimeException e) {
            log.error("Could not compensate availability of book {} by {}", bookId, change, e);
            cause.addSuppressed(e);
        }
    }

    private Loan getLoan(Long loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
    }



}

