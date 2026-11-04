package com.library.user_service.controller;


import com.library.user_service.dtos.UserRequest;
import com.library.user_service.dtos.UserResponse;
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

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id){
        return ResponseEntity.ok(userService.findById(id));
    }


    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> uodate(@PathVariable Long id, @Valid @RequestBody UserRequest request){
        return ResponseEntity.ok(userService.update(id, request));
    }











}
