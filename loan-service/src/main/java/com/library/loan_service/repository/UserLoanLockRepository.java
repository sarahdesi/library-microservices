package com.library.loan_service.repository;

import com.library.loan_service.model.UserLoanLock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UserLoanLockRepository extends JpaRepository<UserLoanLock, Long> {

    // Runs in its own short transaction, before the loan transaction,
    // to avoid deadlocks between concurrent inserts of the same key.
    @Transactional
    @Modifying
    @Query(value = "INSERT IGNORE INTO user_loan_locks (user_id) VALUES (:userId)", nativeQuery = true)
    void ensureExists(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from UserLoanLock l where l.userId = :userId")
    Optional<UserLoanLock> lockByUserId(Long userId);
}
