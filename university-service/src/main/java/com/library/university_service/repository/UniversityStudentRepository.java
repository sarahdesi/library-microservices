package com.library.university_service.repository;

import com.library.university_service.model.UniversityStudent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UniversityStudentRepository extends JpaRepository<UniversityStudent, String> {

    Optional<UniversityStudent> findByRegistrationNumberAndCpfAndActiveTrue(String registrationNumber, String cpf);
}
