package com.library.loan_service.dto;

public record PendingResponse(boolean hasPendingFines, boolean hasOverdueLoans) {
}