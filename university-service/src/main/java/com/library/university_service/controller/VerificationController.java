package com.library.university_service.controller;

import com.library.university_service.dto.StudentResponse;
import com.library.university_service.dto.TeacherResponse;
import com.library.university_service.dto.VerifyRequest;
import com.library.university_service.exception.RecordNotFoundException;
import com.library.university_service.repository.UniversityStudentRepository;
import com.library.university_service.repository.UniversityTeacherRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Answers "is this an active student/teacher with this CPF?".
 * Unknown number, inactive record and wrong CPF all get the same 404, so the
 * response never reveals which piece of data did not match.
 */
@RestController
@RequiredArgsConstructor
public class VerificationController {

    private final UniversityStudentRepository students;
    private final UniversityTeacherRepository teachers;

    @PostMapping("/students/verify")
    public StudentResponse verifyStudent(@Valid @RequestBody VerifyRequest request) {
        return students.findByRegistrationNumberAndCpfAndActiveTrue(request.registrationNumber().trim(), request.cpf())
                .map(StudentResponse::from)
                .orElseThrow(RecordNotFoundException::new);
    }

    @PostMapping("/teachers/verify")
    public TeacherResponse verifyTeacher(@Valid @RequestBody VerifyRequest request) {
        return teachers.findByMaspAndCpfAndActiveTrue(request.registrationNumber().trim(), request.cpf())
                .map(TeacherResponse::from)
                .orElseThrow(RecordNotFoundException::new);
    }
}
