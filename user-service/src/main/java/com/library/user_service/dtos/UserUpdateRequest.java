package com.library.user_service.dtos;

import com.library.user_service.model.Usertype;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// Edicao pelo balcao. O CPF nunca e devolvido pela API, entao aqui ele e opcional:
// ausente ou vazio mantem o CPF atual.
public record UserUpdateRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotNull(message = "Type is required")
        Usertype type,

        @Pattern(regexp = "^(\\d{11})?$", message = "CPF must have 11 digits")
        String cpf
) {}
