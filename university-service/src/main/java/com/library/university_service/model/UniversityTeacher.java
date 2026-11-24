package com.library.university_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Teacher record of the (simulated) academic system. Read-only for the library. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "university_teacher")
public class UniversityTeacher {

    // MASP: matricula de servidor do Estado de Minas Gerais
    @Id
    @Column(length = 20)
    private String masp;

    @Column(nullable = false, length = 11)
    private String cpf;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private boolean active;
}
