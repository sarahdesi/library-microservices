package com.library.user_service.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Aluno: registrationNumber = matricula. Professor: registrationNumber = MASP.
// Sem nome (vem da universidade) e sem role (sempre MEMBER).
public record RegisterUniversityRequest(
        @NotBlank(message = "Registration number is required")
        @Size(max = 20, message = "Registration number must have at most 20 characters")
        String registrationNumber,

        @NotBlank(message = "CPF is required")
        @Pattern(regexp = "^\\d{11}$", message = "CPF must have 11 digits")
        String cpf,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must have at least 8 characters")
        String password
) {}
