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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanService {

    private static final BigDecimal FINE_PER_DAY = new BigDecimal("2.00");

    private final LoanRepository loanRepository;
    private final FineRepository fineRepository;
    private final UserClient userClient;
    private final BookClient bookClient;

    public LoanResponse create(LoanRequest request) {
        UserData user = userClient.getUser(request.userId());
        LoanPolicy policy = LoanPolicy.fromUserType(user.type());

        //RN05: usuario com multa pendente ou livro atrasado nao pode pegar outro livro
        PendingResponse pending = getPending(user.id());
        if (pending.hasPendingFines() || pending.hasOverdueLoans()) {
            throw new BusinessException("User has pending fines or overdue loans");
        }

        //RN03: limite de livros ao mesmo tempo, por tipo de usuario
        long activeLoans = loanRepository.countByUserIdAndStatus(user.id(), LoanStatus.ACTIVE);
        if (activeLoans >= policy.getMaxBooks()) {
            throw new BusinessException("Loan limit reached(" + policy.getMaxBooks() + "books)");
        }

        //RN09: nao pode pegar dois exemplares do mesmo livro
        if (loanRepository.existsByUserIdAndBookIdAndStatus(user.id(), request.bookId(), LoanStatus.ACTIVE)) {
            throw new BusinessException("User already has a copy of this book");
        }

        //RN01: precisa ter exemplar disponivel
        BookData book = bookClient.getBook(request.bookId());
        if (book.availableCopies() <= 0) {
            throw new BusinessException("No copies available");
        }

        //RN02: o emprestimo diminui 1 exemplar disponivel
        bookClient.changeAvailability(book.id(), -1);

        Loan loan = new Loan();
        loan.setUserId(user.id());
        loan.setBookId(book.id());
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(policy.getLoanDays()));
        loan.setRenewalCount(0);
        loan.setStatus(LoanStatus.ACTIVE);
        return LoanResponse.from(loanRepository.save(loan));
    }

    public LoanResponse registerReturn(Long loanId) {
        Loan loan = getLoan(loanId);
        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessException("Loan already returned");
        }

        LocalDate today = LocalDate.now();

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

        //RN02: a devolução aumenta 1 exemplar disponivel
        bookClient.changeAvailability(loan.getBookId(), 1);

        loan.setReturnDate(today);
        loan.setStatus(LoanStatus.RETURNED);
        return LoanResponse.from(loanRepository.save(loan));
    }

    public LoanResponse renew(Long loanId) {
        Loan loan = getLoan(loanId);
        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessException("Returned loans connot be renewed");
        }

        //RN06: emprestimo atrasado nao pode ser renovado
        if (loan.isOverdue()) {
            throw new BusinessException("Overdue loans connot be renewed");
        }

        UserData user = userClient.getUser(loan.getUserId());
        LoanPolicy policy = LoanPolicy.fromUserType(user.type());
        if (loan.getRenewalCount() >= policy.getMaxRenewals()) {
            throw new BusinessException("Renewal limit reached (" + policy.getMaxRenewals() + ")");
        }

        //RN07: a renovação soma um novo prazo ao emprestimo
        loan.setDueDate(loan.getDueDate().plusDays(policy.getLoanDays()));
        loan.setRenewalCount(loan.getRenewalCount() + 1);
        return LoanResponse.from(loanRepository.save(loan));

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

    private Loan getLoan(Long loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
    }



}

