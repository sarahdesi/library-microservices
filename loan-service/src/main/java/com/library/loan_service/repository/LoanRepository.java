package com.library.loan_service.repository;

import com.library.loan_service.model.Loan;
import com.library.loan_service.model.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

        long countByUserIdAndStatus(Long userId, LoanStatus status);

    boolean existsByUserIdAndBookIdAndStatus(Long userId, String bookId, LoanStatus status);

    boolean existsByUserIdAndStatusAndDueDateBefore(Long userId, LoanStatus status, LocalDate date);

    List<Loan> findByUserIdOrderByLoanDateDesc(Long userId);
}
