package com.library.user_service.university;

import java.util.Optional;

/**
 * Academic records of the university. The library only asks whether someone is an
 * active student/teacher with the given CPF; it never depends on how this is stored.
 * Throws {@link UniversityRegistryUnavailableException} when the registry cannot be reached.
 */
public interface UniversityRegistry {

    /** Active student with this registration number (matricula) and CPF */
    Optional<UniversityPerson> findActiveStudent(String registrationNumber, String cpf);

    /** Active teacher with this MASP and CPF */
    Optional<UniversityPerson> findActiveTeacher(String masp, String cpf);
}
