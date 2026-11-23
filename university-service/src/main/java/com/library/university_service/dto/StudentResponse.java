package com.library.university_service.dto;

import com.library.university_service.model.UniversityStudent;

// Never returns the CPF
public record StudentResponse(String registrationNumber, String name, String course) {

    public static StudentResponse from(UniversityStudent student) {
        return new StudentResponse(student.getRegistrationNumber(), student.getName(), student.getCourse());
    }
}
