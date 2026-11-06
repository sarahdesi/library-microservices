package com.library.catalog_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookRequest(

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Author is required")
        String author,

        @NotBlank(message = "Category is required")
        String category,

        @NotBlank(message = "ISBN is required")
        String isbn,

        @NotNull(message = "Total copies is required")
        @Min(value = 1, message = "Total copies must be at least 1")
        Integer totalCopies
) {
}