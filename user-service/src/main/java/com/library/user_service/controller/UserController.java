package com.library.user_service.controller;


import com.library.user_service.dtos.UserRequest;
import com.library.user_service.dtos.UserResponse;
import com.library.user_service.dtos.UserUpdateRequest;
import com.library.user_service.security.CurrentUser;
import com.library.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }


    @GetMapping
    public ResponseEntity<List<UserResponse>> findAll(){
        return ResponseEntity.ok(userService.findAll());
    }

    // Logged-in user (from the token). Mapped before /{id}: literal paths win.
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(CurrentUser currentUser) {
        return ResponseEntity.ok(userService.findById(currentUser.requireId()));
    }

    // Librarian (and other services) see any user; a member only themselves
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id, CurrentUser currentUser){
        currentUser.requireAccessTo(id);
        return ResponseEntity.ok(userService.findById(id));
    }


    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request){
        return ResponseEntity.ok(userService.update(id, request));
    }











}
