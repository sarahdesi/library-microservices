package com.library.user_service.university;

public class UniversityRegistryUnavailableException extends RuntimeException {
    public UniversityRegistryUnavailableException(Throwable cause) {
        super("University registry unavailable", cause);
    }
}
