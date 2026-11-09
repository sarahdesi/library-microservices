package com.library.loan_service.controller;

import com.library.loan_service.dto.LoanRequest;
import com.library.loan_service.dto.LoanResponse;
import com.library.loan_service.dto.PendingResponse;
import com.library.loan_service.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    public ResponseEntity<LoanResponse> create(@Valid @RequestBody LoanRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.create(request));
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<LoanResponse> registerReturn(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.registerReturn(id));

    }
    @PostMapping("/{id}/renew")
    public ResponseEntity<LoanResponse> renew(@PathVariable Long id){
        return ResponseEntity.ok(loanService.renew(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanResponse> findById(@PathVariable Long id){
        return ResponseEntity.ok(loanService.findById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LoanResponse>> findByUser(@PathVariable Long userId){
        return ResponseEntity.ok(loanService.findByUser(userId));
    }

    @GetMapping("/user/{userId}/pending")
    public ResponseEntity<PendingResponse> getPending(@PathVariable Long userId){
        return ResponseEntity.ok(loanService.getPending(userId));
    }





}
