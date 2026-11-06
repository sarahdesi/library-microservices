package com.library.catalog_service.dto;

import jakarta.validation.constraints.NotNull;

public record AvailabilityRequest(
        @NotNull(message = "Change is required")
        Integer change
) {
}
