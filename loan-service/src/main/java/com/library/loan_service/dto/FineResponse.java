package com.library.loan_service.dto;

import com.library.loan_service.model.Fine;

import java.math.BigDecimal;

public record FineResponse(Long id, Long loanId, Long userId, int daysLate, BigDecimal amount, boolean paid) {

    public static FineResponse from(Fine fine) {
        return new FineResponse(
                fine.getId(),
                fine.getLoanId(),
                fine.getUserId(),
                fine.getDaysLate(),
                fine.getAmount(),
                fine.isPaid()
        );
    }
}