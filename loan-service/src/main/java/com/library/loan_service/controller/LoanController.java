package com.library.loan_service.controller;

import com.library.loan_service.dto.LoanRequest;
import com.library.loan_service.dto.LoanResponse;
import com.library.loan_service.dto.PendingResponse;
import com.library.loan_service.security.CurrentUser;
import com.library.loan_service.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    // A member may renew only their own loan (checked inside the transaction)
    @PostMapping("/{id}/renew")
    public ResponseEntity<LoanResponse> renew(@PathVariable Long id, CurrentUser currentUser){
        return ResponseEntity.ok(loanService.renew(id, currentUser));
    }

    // Logged-in member's loans. Literal paths win over /{id}.
    @GetMapping("/me")
    public ResponseEntity<List<LoanResponse>> myLoans(CurrentUser currentUser){
        return ResponseEntity.ok(loanService.findByUser(currentUser.requireId()));
    }

    @GetMapping("/me/pending")
    public ResponseEntity<PendingResponse> myPending(CurrentUser currentUser){
        return ResponseEntity.ok(loanService.getPending(currentUser.requireId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanResponse> findById(@PathVariable Long id, CurrentUser currentUser){
        LoanResponse loan = loanService.findById(id);
        currentUser.requireAccessTo(loan.userId());
        return ResponseEntity.ok(loan);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LoanResponse>> findByUser(@PathVariable Long userId, CurrentUser currentUser){
        currentUser.requireAccessTo(userId);
        return ResponseEntity.ok(loanService.findByUser(userId));
    }

    @GetMapping("/user/{userId}/pending")
    public ResponseEntity<PendingResponse> getPending(@PathVariable Long userId, CurrentUser currentUser){
        currentUser.requireAccessTo(userId);
        return ResponseEntity.ok(loanService.getPending(userId));
    }





}
