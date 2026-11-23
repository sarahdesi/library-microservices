package com.library.university_service.repository;

import com.library.university_service.model.UniversityTeacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UniversityTeacherRepository extends JpaRepository<UniversityTeacher, String> {

    Optional<UniversityTeacher> findByMaspAndCpfAndActiveTrue(String masp, String cpf);
}
