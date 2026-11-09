package com.library.loan_service.exception;

import java.util.List;

public record ErrorResponse(int status, String message, List<String> details) {
}