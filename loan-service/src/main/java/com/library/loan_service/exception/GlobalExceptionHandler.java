package com.library.loan_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
    ErrorResponse body = new ErrorResponse(404, ex.getMessage(), List.of());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
    ErrorResponse body = new ErrorResponse(400, ex.getMessage(), List.of());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    List<String> details = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .toList();
    ErrorResponse body = new ErrorResponse(400, "Validation failed", details);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(ServiceUnavailableException.class)
  public ResponseEntity<ErrorResponse> handleUnavailable(ServiceUnavailableException ex) {
    ErrorResponse body = new ErrorResponse(503, ex.getMessage(), List.of());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
  }
}