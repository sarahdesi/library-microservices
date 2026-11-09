package com.library.catalog_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.ISBN;

public record BookRequest(

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Author is required")
        String author,

        @NotBlank(message = "Category is required")
        String category,

        // Somente dígitos (sem hífens), para que o mesmo livro não seja cadastrado
        // duas vezes com formatações diferentes. @ISBN confere o dígito verificador.
        @NotBlank(message = "ISBN is required")
        @Pattern(regexp = "^(\\d{9}[\\dX]|\\d{13})$", message = "ISBN must have 10 or 13 digits, without hyphens")
        @ISBN(type = ISBN.Type.ANY, message = "ISBN check digit is invalid")
        String isbn,

        @NotNull(message = "Total copies is required")
        @Min(value = 1, message = "Total copies must be at least 1")
        Integer totalCopies
) {
}