package com.library.loan_service.repository;

import com.library.loan_service.model.Fine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FineRepository  extends JpaRepository<Fine, Long> {
    boolean existsByUserIdAndPaidFalse(Long userId);

    List<Fine> findByUserId(Long userId);


}
