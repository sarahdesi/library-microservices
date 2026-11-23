package com.library.loan_service.repository;

import com.library.loan_service.model.Loan;
import com.library.loan_service.model.LoanStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

        long countByUserIdAndStatus(Long userId, LoanStatus status);

    boolean existsByUserIdAndBookIdAndStatus(Long userId, String bookId, LoanStatus status);

    boolean existsByUserIdAndStatusAndDueDateBefore(Long userId, LoanStatus status, LocalDate date);

    List<Loan> findByUserIdOrderByLoanDateDesc(Long userId);

    // SELECT ... FOR UPDATE: concurrent returns/renewals of the same loan wait for each other
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Loan l where l.id = :id")
    Optional<Loan> findByIdForUpdate(Long id);
}
