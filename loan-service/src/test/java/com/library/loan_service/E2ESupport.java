package com.library.loan_service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Talks to the running stack through the API Gateway, like the browser does.
 * The librarian credentials come from -De2e.librarianEmail / -De2e.librarianPassword
 * (the LIBRARIAN_EMAIL / LIBRARIAN_PASSWORD the stack was started with).
 */
final class E2ESupport {

    final String baseUrl = System.getProperty("e2e.baseUrl", "http://localhost:8080");
    final HttpClient http = HttpClient.newHttpClient();
    final JsonMapper json = JsonMapper.builder().build();

    String librarianToken() throws Exception {
        String email = System.getProperty("e2e.librarianEmail");
        String password = System.getProperty("e2e.librarianPassword");
        assertThat(email).as("-De2e.librarianEmail is required").isNotBlank();
        assertThat(password).as("-De2e.librarianPassword is required").isNotBlank();
        return login(email, password);
    }

    String login(String email, String password) throws Exception {
        HttpResponse<String> response = send("POST", "/auth/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return json.readTree(response.body()).get("token").asString();
    }

    String createBook(String token, int totalCopies) throws Exception {
        String body = """
                {"title":"E2E %s","author":"Test","category":"Test","isbn":"%s","totalCopies":%d}
                """.formatted(UUID.randomUUID(), randomIsbn13(), totalCopies);
        HttpResponse<String> response = send("POST", "/books", token, body);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        return json.readTree(response.body()).get("id").asString();
    }

    /** Counter registration (no password: the member has no access until they activate it) */
    long createMember(String token, String type) throws Exception {
        String body = """
                {"name":"E2E member","email":"e2e-%s@test.local","type":"%s","cpf":"%s"}
                """.formatted(UUID.randomUUID(), type, randomCpf());
        HttpResponse<String> response = send("POST", "/users", token, body);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        return json.readTree(response.body()).get("id").asLong();
    }

    JsonNode get(String path, String token) throws Exception {
        return json.readTree(send("GET", path, token, null).body());
    }

    HttpResponse<String> send(String method, String path, String token, String body, String... headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (headers.length > 0) {
            builder.headers(headers);
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    static String randomCpf() {
        return String.format("%011d", ThreadLocalRandom.current().nextLong(1, 99_999_999_999L));
    }

    static String randomIsbn13() {
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
}
