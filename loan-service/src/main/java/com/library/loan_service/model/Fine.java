package com.library.loan_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "fines")
public class Fine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long loanId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int daysLate;

    @Column(nullable = false, precision = 10 , scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private boolean paid;
}
