package com.library.user_service.dtos;

public record LoginResponse(String token, UserResponse user) {}
