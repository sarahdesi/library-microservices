package com.library.user_service.repository;

import com.library.user_service.model.Role;
import com.library.user_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

    boolean existsByRole(Role role);

    Optional<User> findByEmail(String email);

    Optional<User> findByCpf(String cpf);
}
