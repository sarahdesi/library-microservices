package com.library.university_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// CPF goes in the body, never in the URL, so it does not show up in access logs
public record VerifyRequest(
        @NotBlank(message = "Registration number is required")
        String registrationNumber,

        @NotBlank(message = "CPF is required")
        @Pattern(regexp = "^\\d{11}$", message = "CPF must have 11 digits")
        String cpf
) {}
