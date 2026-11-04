package com.library.user_service.dtos;

import com.library.user_service.model.User;
import com.library.user_service.model.Usertype;

public record UserResponse(Long id, String name, String email, Usertype type) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getType());
    }
}