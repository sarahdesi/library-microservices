package com.library.user_service.dtos;

import com.library.user_service.model.Role;
import com.library.user_service.model.User;
import com.library.user_service.model.Usertype;

// Nunca expoe senha, hash nem CPF
public record UserResponse(Long id, String name, String email, Usertype type, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getType(), user.getRole());
    }
}
