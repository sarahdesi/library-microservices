package com.library.loan_service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end race tests against the running stack (docker compose), through the API Gateway.
 * Disabled by default. Run with:
 *   docker compose up -d --build api-gateway
 *   ./mvnw test -Dtest=LoanConcurrencyE2ETests -De2e=true [-De2e.baseUrl=http://localhost:8080]
 * Each run creates test users (there is no endpoint to delete users) and removes its books at the end.
 */
@EnabledIfSystemProperty(named = "e2e", matches = "true")
class LoanConcurrencyE2ETests {

    private static final int THREADS = 10;

    private final String baseUrl = System.getProperty("e2e.baseUrl", "http://localhost:8080");
    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void concurrentLoansOfLastCopyLetOnlyOneThrough() throws Exception {
        String bookId = createBook(1);
        List<Long> userIds = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            userIds.add(createUser());
        }
        try {
            List<HttpResponse<String>> responses = ConcurrentRunner.run(THREADS, i ->
                    post("/loans", "{\"userId\":" + userIds.get(i) + ",\"bookId\":\"" + bookId + "\"}"));

            assertThat(responses).filteredOn(r -> r.statusCode() == 201).hasSize(1);
            assertThat(responses).filteredOn(r -> r.statusCode() == 400).hasSize(THREADS - 1)
                    .allSatisfy(r -> assertThat(r.body()).contains("No copies available"));
            assertThat(getBook(bookId).get("availableCopies").asInt()).isZero();
            assertThat(activeLoanIds(userIds, bookId)).hasSize(1);
        } finally {
            cleanUp(userIds, bookId);
        }
    }

    @Test
    void concurrentReturnsOfSameLoanHaveOneWinner() throws Exception {
        String bookId = createBook(1);
        long userId = createUser();
        try {
            HttpResponse<String> created = post("/loans", "{\"userId\":" + userId + ",\"bookId\":\"" + bookId + "\"}");
            assertThat(created.statusCode()).isEqualTo(201);
            long loanId = json.readTree(created.body()).get("id").asLong();

            List<HttpResponse<String>> responses =
                    ConcurrentRunner.run(2, i -> post("/loans/" + loanId + "/return", null));

            assertThat(responses).filteredOn(r -> r.statusCode() == 200).hasSize(1);
            assertThat(responses).filteredOn(r -> r.statusCode() == 400).hasSize(1)
                    .allSatisfy(r -> assertThat(r.body()).contains("Loan already returned"));
            JsonNode book = getBook(bookId);
            assertThat(book.get("availableCopies").asInt()).isEqualTo(book.get("totalCopies").asInt());
        } finally {
            cleanUp(List.of(userId), bookId);
        }
    }

    // ---------- helpers ----------

    private String createBook(int totalCopies) throws Exception {
        String body = """
                {"title":"E2E concurrency %s","author":"Test","category":"Test","isbn":"%s","totalCopies":%d}
                """.formatted(UUID.randomUUID(), randomIsbn13(), totalCopies);
        HttpResponse<String> response = post("/books", body);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        return json.readTree(response.body()).get("id").asString();
    }

    private long createUser() throws Exception {
        String body = """
                {"name":"E2E concurrency","email":"e2e-%s@test.local","type":"STUDENT"}
                """.formatted(UUID.randomUUID());
        HttpResponse<String> response = post("/users", body);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        return json.readTree(response.body()).get("id").asLong();
    }

    private JsonNode getBook(String bookId) throws Exception {
        return json.readTree(send(HttpRequest.newBuilder(uri("/books/" + bookId)).GET().build()).body());
    }

    private List<Long> activeLoanIds(List<Long> userIds, String bookId) throws Exception {
        List<Long> ids = new ArrayList<>();
        for (Long userId : userIds) {
            JsonNode loans = json.readTree(send(HttpRequest.newBuilder(uri("/loans/user/" + userId)).GET().build()).body());
            for (JsonNode loan : loans) {
                if (bookId.equals(loan.get("bookId").asString()) && "ACTIVE".equals(loan.get("status").asString())) {
                    ids.add(loan.get("id").asLong());
                }
            }
        }
        return ids;
    }

    /** Best effort: returns the test loans and deletes the test book. */
    private void cleanUp(List<Long> userIds, String bookId) throws Exception {
        for (Long loanId : activeLoanIds(userIds, bookId)) {
            post("/loans/" + loanId + "/return", null);
        }
        send(HttpRequest.newBuilder(uri("/books/" + bookId)).DELETE().build());
    }

    private static String randomIsbn13() {
        StringBuilder digits = new StringBuilder("978");
        for (int i = 0; i < 9; i++) {
            digits.append(ThreadLocalRandom.current().nextInt(10));
        }
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += (digits.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return digits.append((10 - sum % 10) % 10).toString();
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path));
        builder = body == null
                ? builder.POST(HttpRequest.BodyPublishers.noBody())
                : builder.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        return send(builder.build());
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create(baseUrl + path);
    }
}
