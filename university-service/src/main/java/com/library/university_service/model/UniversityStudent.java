package com.library.university_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Student record of the (simulated) academic system. Read-only for the library. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "university_student")
public class UniversityStudent {

    // matricula
    @Id
    @Column(name = "registration_number", length = 20)
    private String registrationNumber;

    @Column(nullable = false, length = 11)
    private String cpf;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String course;

    @Column(nullable = false)
    private boolean active;
}
