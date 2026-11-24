package com.library.loan_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One row per user, locked with SELECT ... FOR UPDATE while a new loan is created,
 * so loans of the same user are checked (limit, same book, pending) one at a time.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "user_loan_locks")
public class UserLoanLock {

    @Id
    @Column(name = "user_id")
    private Long userId;
}
