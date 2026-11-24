package com.library.university_service.exception;

public class RecordNotFoundException extends RuntimeException {
    public RecordNotFoundException() {
        super("Record not found");
    }
}
