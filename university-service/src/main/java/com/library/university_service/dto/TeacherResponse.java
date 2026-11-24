package com.library.university_service.dto;

import com.library.university_service.model.UniversityTeacher;

// Never returns the CPF. registrationNumber holds the MASP.
public record TeacherResponse(String registrationNumber, String name, String department) {

    public static TeacherResponse from(UniversityTeacher teacher) {
        return new TeacherResponse(teacher.getMasp(), teacher.getName(), teacher.getDepartment());
    }
}
