package com.library.loan_service.controller;


import com.library.loan_service.dto.FineResponse;
import com.library.loan_service.service.FineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fines")
@RequiredArgsConstructor
public class FineController {

    private final FineService fineService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FineResponse>> findByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(fineService.findByUser(userId));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<FineResponse> pay(@PathVariable Long id) {
        return ResponseEntity.ok(fineService.pay(id));
    }

}
