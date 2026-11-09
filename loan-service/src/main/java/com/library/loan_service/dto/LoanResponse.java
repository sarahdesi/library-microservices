package com.library.loan_service.dto;

import com.library.loan_service.model.Loan;
import com.library.loan_service.model.LoanStatus;

import java.time.LocalDate;

public record LoanResponse(
        Long id,
        Long userId,
        String bookId,
        LocalDate loanDate,
        LocalDate dueDate,
        LocalDate returnDate,
        int renewalCount,
        LoanStatus status,
        boolean overdue
) {

    public static LoanResponse from(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getUserId(),
                loan.getBookId(),
                loan.getLoanDate(),
                loan.getDueDate(),
                loan.getReturnDate(),
                loan.getRenewalCount(),
                loan.getStatus(),
                loan.isOverdue()
        );
    }
}