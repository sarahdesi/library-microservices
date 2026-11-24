package com.library.user_service;

import com.library.user_service.config.LibrarianSeeder;
import com.library.user_service.config.SchemaUpgrade;
import com.library.user_service.model.Role;
import com.library.user_service.model.User;
import com.library.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "app.jwt.secret=" + AuthTests.SECRET,
                "app.librarian.email=" + AuthTests.LIBRARIAN_EMAIL,
                "app.librarian.password=" + AuthTests.LIBRARIAN_PASSWORD
        })
class AuthTests {

    static final String SECRET = "test-secret-with-at-least-32-characters!!";
    static final String LIBRARIAN_EMAIL = "librarian@test.local";
    static final String LIBRARIAN_PASSWORD = "librarian-pass";

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Container
    @ServiceConnection
    static MariaDBContainer mariadb = new MariaDBContainer("mariadb:11");

    @LocalServerPort
    int port;

    @Autowired
    UserRepository userRepository;

    @Autowired
    LibrarianSeeder librarianSeeder;

    @Autowired
    SchemaUpgrade schemaUpgrade;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void keepOnlyLibrarian() {
        userRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.LIBRARIAN)
                .forEach(userRepository::delete);
    }

    // ---------- librarian seed ----------

    @Test
    void librarianIsSeededOnceAndCanLogIn() throws Exception {
        librarianSeeder.run(new DefaultApplicationArguments());
        assertThat(userRepository.findAll()).filteredOn(u -> u.getRole() == Role.LIBRARIAN).hasSize(1);

        HttpResponse<String> response = login(LIBRARIAN_EMAIL, LIBRARIAN_PASSWORD);

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = json.readTree(response.body());
        assertThat(body.get("user").get("role").asString()).isEqualTo("LIBRARIAN");
        assertThat(body.get("user").get("type").isNull()).isTrue();

        Jwt jwt = decode(body.get("token").asString());
        assertThat(jwt.getSubject()).isEqualTo(body.get("user").get("id").asString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("LIBRARIAN");
        assertThat(jwt.hasClaim("type")).isFalse();
        assertThat(jwt.getExpiresAt()).isCloseTo(Instant.now().plus(Duration.ofHours(1)), within(Duration.ofMinutes(1)));
    }

    @Test
    void schemaUpgradeMakesTypeNullableOnOldDatabases() {
        // simulate the old schema: type NOT NULL (no librarian row can exist then)
        userRepository.findAll().forEach(userRepository::delete);
        jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN type enum('EXTERNAL','STUDENT','TEACHER') NOT NULL");

        schemaUpgrade.run(new DefaultApplicationArguments());
        schemaUpgrade.run(new DefaultApplicationArguments()); // idempotent
        librarianSeeder.run(new DefaultApplicationArguments());

        assertThat(userRepository.existsByRole(Role.LIBRARIAN)).isTrue();
    }

    // ---------- login ----------

    @Test
    void wrongPasswordUnknownEmailAndCounterAccountGetTheSame401() throws Exception {
        String cpf = cpf();
        String email = email();
        assertThat(post("/users", counterUser(email, cpf, "EXTERNAL")).statusCode()).isEqualTo(201);

        HttpResponse<String> wrongPassword = login(LIBRARIAN_EMAIL, "wrong-password");
        HttpResponse<String> unknownEmail = login("nobody@test.local", "whatever-pass");
        HttpResponse<String> noPassword = login(email, "whatever-pass");

        for (HttpResponse<String> r : new HttpResponse[]{wrongPassword, unknownEmail, noPassword}) {
            assertThat(r.statusCode()).isEqualTo(401);
            assertThat(json.readTree(r.body()).get("message").asString()).isEqualTo("Invalid credentials");
        }
        assertThat(unknownEmail.body()).isEqualTo(wrongPassword.body()).isEqualTo(noPassword.body());
    }

    // ---------- external self-registration ----------

    @Test
    void externalRegistersAndLogsInAsMember() throws Exception {
        String email = email();
        HttpResponse<String> created = post("/auth/register/external", external("Ana", email, cpf(), "senha-forte"));

        assertThat(created.statusCode()).isEqualTo(201);
        JsonNode user = json.readTree(created.body());
        assertThat(user.get("role").asString()).isEqualTo("MEMBER");
        assertThat(user.get("type").asString()).isEqualTo("EXTERNAL");
        assertThat(user.has("cpf")).isFalse();
        assertThat(user.has("password")).isFalse();

        HttpResponse<String> logged = login(email, "senha-forte");
        assertThat(logged.statusCode()).isEqualTo(200);
        Jwt jwt = decode(json.readTree(logged.body()).get("token").asString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("MEMBER");
        assertThat(jwt.getClaimAsString("type")).isEqualTo("EXTERNAL");
        assertThat(logged.body()).doesNotContain("password").doesNotContain("cpf");
    }

    @Test
    void roleInPublicRegistrationIsIgnored() throws Exception {
        String body = """
                {"name":"Eve","email":"%s","cpf":"%s","password":"senha-forte","role":"LIBRARIAN"}
                """.formatted(email(), cpf());

        HttpResponse<String> response = post("/auth/register/external", body);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(json.readTree(response.body()).get("role").asString()).isEqualTo("MEMBER");
    }

    @Test
    void duplicateCpfReturns409() throws Exception {
        String cpf = cpf();
        post("/auth/register/external", external("Ana", email(), cpf, "senha-forte"));

        HttpResponse<String> response = post("/auth/register/external", external("Bia", email(), cpf, "senha-forte"));

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("CPF already registered");
    }

    @Test
    void duplicateEmailReturnsExistingError() throws Exception {
        String email = email();
        post("/auth/register/external", external("Ana", email, cpf(), "senha-forte"));

        HttpResponse<String> response = post("/auth/register/external", external("Bia", email, cpf(), "senha-forte"));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("Email already registered");
    }

    @Test
    void shortPasswordAndInvalidCpfAreRejected() throws Exception {
        HttpResponse<String> response = post("/auth/register/external", external("Ana", email(), "123", "curta"));

        assertThat(response.statusCode()).isEqualTo(400);
        JsonNode body = json.readTree(response.body());
        assertThat(body.get("message").asString()).isEqualTo("Validation failed");
        assertThat(body.get("details").toString())
                .contains("password: Password must have at least 8 characters")
                .contains("cpf: CPF must have 11 digits");
    }

    // ---------- activation of a counter account ----------

    @Test
    void counterAccountIsActivatedWithSameCpfEmailAndType() throws Exception {
        String cpf = cpf();
        String email = email();
        long counterId = json.readTree(post("/users", counterUser(email, cpf, "EXTERNAL")).body()).get("id").asLong();

        HttpResponse<String> response = post("/auth/register/external", external("Outro nome", email, cpf, "senha-forte"));

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode user = json.readTree(response.body());
        assertThat(user.get("id").asLong()).isEqualTo(counterId);
        assertThat(user.get("name").asString()).isEqualTo("Balcao"); // name checked at the counter is kept
        assertThat(login(email, "senha-forte").statusCode()).isEqualTo(200);
        assertThat(userRepository.count()).isEqualTo(2); // librarian + this member
    }

    @Test
    void counterAccountIsNotActivatedWithDifferentEmailTypeOrExistingPassword() throws Exception {
        String cpf = cpf();
        String email = email();
        post("/users", counterUser(email, cpf, "EXTERNAL"));
        String studentCpf = cpf();
        String studentEmail = email();
        post("/users", counterUser(studentEmail, studentCpf, "STUDENT"));

        HttpResponse<String> otherEmail = post("/auth/register/external", external("X", email(), cpf, "senha-forte"));
        HttpResponse<String> otherType = post("/auth/register/external", external("X", studentEmail, studentCpf, "senha-forte"));
        post("/auth/register/external", external("X", email, cpf, "senha-forte")); // activates
        HttpResponse<String> again = post("/auth/register/external", external("X", email, cpf, "outra-senha"));

        for (HttpResponse<String> r : new HttpResponse[]{otherEmail, otherType, again}) {
            assertThat(r.statusCode()).isEqualTo(409);
            assertThat(r.body()).contains("CPF already registered");
        }
        assertThat(login(email, "outra-senha").statusCode()).isEqualTo(401);
    }

    // ---------- counter registration ----------

    @Test
    void counterRegistrationRequiresCpfAndCreatesMemberWithoutAccess() throws Exception {
        HttpResponse<String> withoutCpf = post("/users", """
                {"name":"Balcao","email":"%s","type":"STUDENT"}
                """.formatted(email()));
        assertThat(withoutCpf.statusCode()).isEqualTo(400);
        assertThat(withoutCpf.body()).contains("cpf: CPF is required");

        HttpResponse<String> created = post("/users", counterUser(email(), cpf(), "STUDENT"));
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(json.readTree(created.body()).get("role").asString()).isEqualTo("MEMBER");
        User saved = userRepository.findById(json.readTree(created.body()).get("id").asLong()).orElseThrow();
        assertThat(saved.getPassword()).isNull();
    }

    @Test
    void editingWithoutCpfKeepsTheCurrentOne() throws Exception {
        String cpf = cpf();
        long id = json.readTree(post("/users", counterUser(email(), cpf, "STUDENT")).body()).get("id").asLong();

        HttpResponse<String> response = put("/users/" + id, """
                {"name":"Novo nome","email":"%s","type":"TEACHER"}
                """.formatted(email()));

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        User saved = userRepository.findById(id).orElseThrow();
        assertThat(saved.getCpf()).isEqualTo(cpf);
        assertThat(saved.getName()).isEqualTo("Novo nome");

        HttpResponse<String> invalidCpf = put("/users/" + id, """
                {"name":"Novo nome","email":"%s","type":"TEACHER","cpf":"123"}
                """.formatted(saved.getEmail()));
        assertThat(invalidCpf.statusCode()).isEqualTo(400);
    }

    // ---------- helpers ----------

    private Jwt decode(String token) {
        return NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .build()
                .decode(token);
    }

    private static String email() {
        return "user" + SEQ.incrementAndGet() + "-" + System.nanoTime() + "@test.local";
    }

    private static String cpf() {
        return String.format("%011d", ThreadLocalRandom.current().nextLong(1, 99_999_999_999L));
    }

    private static String external(String name, String email, String cpf, String password) {
        return """
                {"name":"%s","email":"%s","cpf":"%s","password":"%s"}
                """.formatted(name, email, cpf, password);
    }

    private static String counterUser(String email, String cpf, String type) {
        return """
                {"name":"Balcao","email":"%s","cpf":"%s","type":"%s"}
                """.formatted(email, cpf, type);
    }

    private HttpResponse<String> login(String email, String password) throws Exception {
        return post("/auth/login", """
                {"email":"%s","password":"%s"}
                """.formatted(email, password));
    }

    private HttpResponse<String> put(String path, String body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
