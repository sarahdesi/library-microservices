package com.library.loan_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoanRequest(
        @NotNull(message = "User id is required")
        Long userId,

        @NotBlank(message = "Book id is required")
        String bookId
) {
}
