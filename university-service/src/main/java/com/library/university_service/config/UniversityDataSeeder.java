package com.library.university_service.config;

import com.library.university_service.model.UniversityStudent;
import com.library.university_service.model.UniversityTeacher;
import com.library.university_service.repository.UniversityStudentRepository;
import com.library.university_service.repository.UniversityTeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Test data (fictitious people and CPFs), inserted only when the tables are empty.
 * Documented in the README so the self-registration can be tried out.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UniversityDataSeeder implements ApplicationRunner {

    private final UniversityStudentRepository students;
    private final UniversityTeacherRepository teachers;

    @Override
    public void run(ApplicationArguments args) {
        if (students.count() == 0) {
            students.saveAll(List.of(
                    new UniversityStudent("2024001", "11111111111", "Ana Beatriz Souza", "Ciência da Computação", true),
                    new UniversityStudent("2024002", "22222222222", "Bruno Henrique Lima", "Engenharia Civil", true),
                    new UniversityStudent("2024003", "33333333333", "Carla Mendes Rocha", "Pedagogia", true),
                    new UniversityStudent("2019050", "44444444444", "Diego Ferreira Alves", "Direito", false)));
            log.info("University students seeded");
        }
        if (teachers.count() == 0) {
            teachers.saveAll(List.of(
                    new UniversityTeacher("10203040", "55555555555", "Eduardo Castro Nunes", "Departamento de Computação", true),
                    new UniversityTeacher("10203041", "66666666666", "Fernanda Rocha Dias", "Departamento de Letras", true),
                    new UniversityTeacher("90807060", "77777777777", "Gustavo Almeida Reis", "Departamento de Matemática", false)));
            log.info("University teachers seeded");
        }
    }
}
