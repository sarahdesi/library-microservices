package com.library.user_service;

import com.library.user_service.model.Role;
import com.library.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/** "Only your own" in the user-service, with the headers the gateway would set */
@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"eureka.client.enabled=false", "app.jwt.secret=test-secret-with-at-least-32-characters!!"})
class UserAccessTests {

    @Container
    @ServiceConnection
    static MariaDBContainer mariadb = new MariaDBContainer("mariadb:11");

    @LocalServerPort
    int port;

    @Autowired
    UserRepository userRepository;

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();
    private long ana;
    private long bruno;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.findAll().stream().filter(u -> u.getRole() != Role.LIBRARIAN).forEach(userRepository::delete);
        ana = createMember("Ana");
        bruno = createMember("Bruno");
    }

    @Test
    void meReturnsTheLoggedInUser() throws Exception {
        HttpResponse<String> response = get("/users/me", "X-User-Id", String.valueOf(ana), "X-User-Role", "MEMBER");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json.readTree(response.body()).get("name").asString()).isEqualTo("Ana");
    }

    @Test
    void meWithoutIdentityIs401() throws Exception {
        assertThat(get("/users/me").statusCode()).isEqualTo(401);
    }

    @Test
    void memberSeesOnlyThemselves() throws Exception {
        assertThat(get("/users/" + ana, "X-User-Id", String.valueOf(ana), "X-User-Role", "MEMBER").statusCode())
                .isEqualTo(200);
        HttpResponse<String> other = get("/users/" + bruno, "X-User-Id", String.valueOf(ana), "X-User-Role", "MEMBER");
        assertThat(other.statusCode()).isEqualTo(403);
        assertThat(other.body()).contains("Access denied");
    }

    @Test
    void librarianAndInternalCallsSeeAnyUser() throws Exception {
        assertThat(get("/users/" + bruno, "X-User-Id", "999", "X-User-Role", "LIBRARIAN").statusCode()).isEqualTo(200);
        // the loan-service calls /users/{id} through Eureka without identity headers
        assertThat(get("/users/" + bruno).statusCode()).isEqualTo(200);
    }

    private long createMember(String name) throws Exception {
        String cpf = String.format("%011d", ThreadLocalRandom.current().nextLong(1, 99_999_999_999L));
        String body = """
                {"name":"%s","email":"%s-%d@test.local","cpf":"%s","type":"STUDENT"}
                """.formatted(name, name, System.nanoTime(), cpf);
        HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/users"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
        return json.readTree(response.body()).get("id").asLong();
    }

    private HttpResponse<String> get(String path, String... headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (headers.length > 0) {
            builder.headers(headers);
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
