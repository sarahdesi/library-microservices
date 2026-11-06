package com.library.catalog_service.exception;

import java.util.List;

public record ErrorResponse(int status, String error, List<String> details) {
}