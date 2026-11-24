package com.library.loan_service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import tools.jackson.databind.JsonNode;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end race tests against the running stack (docker compose), through the API Gateway.
 * Disabled by default. Run with:
 *   ./mvnw test -Dtest='*E2ETests' -De2e=true -De2e.librarianEmail=... -De2e.librarianPassword=...
 * Each run creates test users (there is no endpoint to delete users) and removes its books at the end.
 */
@EnabledIfSystemProperty(named = "e2e", matches = "true")
class LoanConcurrencyE2ETests {

    private static final int THREADS = 10;

    private final E2ESupport e2e = new E2ESupport();
    private String librarian;

    @BeforeEach
    void login() throws Exception {
        librarian = e2e.librarianToken();
    }

    @Test
    void concurrentLoansOfLastCopyLetOnlyOneThrough() throws Exception {
        String bookId = e2e.createBook(librarian, 1);
        List<Long> userIds = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            userIds.add(e2e.createMember(librarian, "STUDENT"));
        }
        try {
            List<HttpResponse<String>> responses = ConcurrentRunner.run(THREADS, i ->
                    e2e.send("POST", "/loans", librarian, "{\"userId\":" + userIds.get(i) + ",\"bookId\":\"" + bookId + "\"}"));

            assertThat(responses).filteredOn(r -> r.statusCode() == 201).hasSize(1);
            assertThat(responses).filteredOn(r -> r.statusCode() == 400).hasSize(THREADS - 1)
                    .allSatisfy(r -> assertThat(r.body()).contains("No copies available"));
            assertThat(e2e.get("/books/" + bookId, librarian).get("availableCopies").asInt()).isZero();
            assertThat(activeLoanIds(userIds, bookId)).hasSize(1);
        } finally {
            cleanUp(userIds, bookId);
        }
    }

    @Test
    void concurrentReturnsOfSameLoanHaveOneWinner() throws Exception {
        String bookId = e2e.createBook(librarian, 1);
        long userId = e2e.createMember(librarian, "STUDENT");
        try {
            HttpResponse<String> created = e2e.send("POST", "/loans", librarian,
                    "{\"userId\":" + userId + ",\"bookId\":\"" + bookId + "\"}");
            assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
            long loanId = e2e.json.readTree(created.body()).get("id").asLong();

            List<HttpResponse<String>> responses =
                    ConcurrentRunner.run(2, i -> e2e.send("POST", "/loans/" + loanId + "/return", librarian, null));

            assertThat(responses).filteredOn(r -> r.statusCode() == 200).hasSize(1);
            assertThat(responses).filteredOn(r -> r.statusCode() == 400).hasSize(1)
                    .allSatisfy(r -> assertThat(r.body()).contains("Loan already returned"));
            JsonNode book = e2e.get("/books/" + bookId, librarian);
            assertThat(book.get("availableCopies").asInt()).isEqualTo(book.get("totalCopies").asInt());
        } finally {
            cleanUp(List.of(userId), bookId);
        }
    }

    private List<Long> activeLoanIds(List<Long> userIds, String bookId) throws Exception {
        List<Long> ids = new ArrayList<>();
        for (Long userId : userIds) {
            for (JsonNode loan : e2e.get("/loans/user/" + userId, librarian)) {
                if (bookId.equals(loan.get("bookId").asString()) && "ACTIVE".equals(loan.get("status").asString())) {
                    ids.add(loan.get("id").asLong());
                }
            }
        }
        return ids;
    }

    /** Best effort: returns the test loans and deletes the test book */
    private void cleanUp(List<Long> userIds, String bookId) throws Exception {
        for (Long loanId : activeLoanIds(userIds, bookId)) {
            e2e.send("POST", "/loans/" + loanId + "/return", librarian, null);
        }
        e2e.send("DELETE", "/books/" + bookId, librarian, null);
    }
}
