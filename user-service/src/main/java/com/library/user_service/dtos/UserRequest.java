package com.library.user_service.dtos;

import com.library.user_service.model.Usertype;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// Cadastro feito pela bibliotecaria no balcao: o membro nasce sem senha (sem acesso)
public record UserRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotNull(message = "Type is required")
        Usertype type,

        @NotBlank(message = "CPF is required")
        @Pattern(regexp = "^\\d{11}$", message = "CPF must have 11 digits")
        String cpf
) {}
