package com.library.loan_service.dto;

// type is null for the librarian (role LIBRARIAN)
public record UserData(Long id, String name, String email, String type, String role) {
}
