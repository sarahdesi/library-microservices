package com.library.user_service.controller;

import com.library.user_service.dtos.LoginRequest;
import com.library.user_service.dtos.LoginResponse;
import com.library.user_service.dtos.RegisterExternalRequest;
import com.library.user_service.dtos.RegisterUniversityRequest;
import com.library.user_service.dtos.UserResponse;
import com.library.user_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register/external")
    public ResponseEntity<UserResponse> registerExternal(@Valid @RequestBody RegisterExternalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerExternal(request));
    }

    @PostMapping("/register/student")
    public ResponseEntity<UserResponse> registerStudent(@Valid @RequestBody RegisterUniversityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerStudent(request));
    }

    @PostMapping("/register/teacher")
    public ResponseEntity<UserResponse> registerTeacher(@Valid @RequestBody RegisterUniversityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerTeacher(request));
    }
}
