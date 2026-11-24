package com.library.user_service.config;

import com.library.user_service.model.Role;
import com.library.user_service.model.User;
import com.library.user_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first librarian from LIBRARIAN_EMAIL / LIBRARIAN_PASSWORD, only if there is none.
 * There is no endpoint to create librarians. The password is never logged.
 */
@Slf4j
@Component
@Order(1)
public class LibrarianSeeder implements ApplicationRunner {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String name;

    public LibrarianSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           @Value("${app.librarian.email:}") String email,
                           @Value("${app.librarian.password:}") String password,
                           @Value("${app.librarian.name:Bibliotecária}") String name) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.LIBRARIAN)) {
            return;
        }
        if (email.isBlank() || password.isBlank()) {
            log.warn("No librarian account exists and LIBRARIAN_EMAIL / LIBRARIAN_PASSWORD are not set");
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            log.warn("LIBRARIAN_PASSWORD must have at least {} characters; librarian not created", MIN_PASSWORD_LENGTH);
            return;
        }
        if (userRepository.existsByEmail(email)) {
            log.warn("LIBRARIAN_EMAIL is already used by another account; librarian not created");
            return;
        }
        User librarian = new User();
        librarian.setName(name);
        librarian.setEmail(email);
        librarian.setRole(Role.LIBRARIAN);
        librarian.setPassword(passwordEncoder.encode(password));
        userRepository.save(librarian);
        log.info("Initial librarian account created");
    }
}
