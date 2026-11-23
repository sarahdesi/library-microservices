package com.library.user_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * ddl-auto=update adds new columns but never relaxes an existing NOT NULL.
 * users.type was created NOT NULL, and it must be nullable for LIBRARIAN.
 * Idempotent: only runs the ALTER when the column is still NOT NULL.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class SchemaUpgrade implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        String nullable = jdbcTemplate.queryForObject("""
                SELECT IS_NULLABLE FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'type'
                """, String.class);
        if ("NO".equals(nullable)) {
            jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN type enum('EXTERNAL','STUDENT','TEACHER') NULL");
            log.info("users.type is now nullable (required for librarian accounts)");
        }
    }
}
