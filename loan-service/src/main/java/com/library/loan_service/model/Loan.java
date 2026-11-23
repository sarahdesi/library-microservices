package com.library.loan_service.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String bookId;

    @Column(nullable = false)
    private LocalDate loanDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate returnDate;

    @Column(nullable = false)
    private int renewalCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    // Optimistic lock: an update based on a stale read fails instead of overwriting.
    // The default fills existing rows when ddl-auto adds the column.
    @Version
    @Column(columnDefinition = "bigint not null default 0")
    private long version;

    public boolean isOverdue(){
        return status == LoanStatus.ACTIVE && LocalDate.now().isAfter(dueDate);
    }


}

