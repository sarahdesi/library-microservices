package com.library.user_service;

import com.library.user_service.model.Role;
import com.library.user_service.model.User;
import com.library.user_service.repository.UserRepository;
import com.library.user_service.university.UniversityPerson;
import com.library.user_service.university.UniversityRegistry;
import com.library.user_service.university.UniversityRegistryUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"eureka.client.enabled=false", "app.jwt.secret=test-secret-with-at-least-32-characters!!"})
class UniversityRegistrationTests {

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final String PASSWORD = "senha-forte";

    @Container
    @ServiceConnection
    static MariaDBContainer mariadb = new MariaDBContainer("mariadb:11");

    @LocalServerPort
    int port;

    @Autowired
    UserRepository userRepository;

    @MockitoBean
    UniversityRegistry universityRegistry;

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void clean() {
        userRepository.findAll().stream().filter(u -> u.getRole() != Role.LIBRARIAN).forEach(userRepository::delete);
    }

    // ---------- registration ----------

    @Test
    void studentRegistersWithNameFromUniversityAndLogsIn() throws Exception {
        String cpf = cpf();
        String email = email();
        studentExists("2024001", cpf, "Ana Beatriz Souza");

        HttpResponse<String> created = post("/auth/register/student", university("2024001", cpf, email));

        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        JsonNode user = json.readTree(created.body());
        assertThat(user.get("name").asString()).isEqualTo("Ana Beatriz Souza");
        assertThat(user.get("type").asString()).isEqualTo("STUDENT");
        assertThat(user.get("role").asString()).isEqualTo("MEMBER");
        assertThat(created.body()).doesNotContain(cpf).doesNotContain("registrationNumber");

        assertThat(login(email).statusCode()).isEqualTo(200);
    }

    @Test
    void teacherIsCheckedAgainstTheTeacherBase() throws Exception {
        String cpf = cpf();
        when(universityRegistry.findActiveTeacher(eq("10203040"), eq(cpf)))
                .thenReturn(Optional.of(new UniversityPerson("10203040", "Eduardo Castro Nunes", "Computação")));

        HttpResponse<String> created = post("/auth/register/teacher", university("10203040", cpf, email()));

        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(json.readTree(created.body()).get("type").asString()).isEqualTo("TEACHER");
        verify(universityRegistry, never()).findActiveStudent(anyString(), anyString());
    }

    @Test
    void dataThatDoesNotMatchTheUniversityIsRejected() throws Exception {
        // unknown number, inactive or wrong CPF: the registry answers "not found" for all of them
        when(universityRegistry.findActiveStudent(anyString(), anyString())).thenReturn(Optional.empty());

        HttpResponse<String> response = post("/auth/register/student", university("2019050", cpf(), email()));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(message(response)).isEqualTo("University data does not match");
        assertThat(userRepository.count()).isZero();
    }

    @Test
    void registrationNumberAlreadyRegisteredReturns409() throws Exception {
        String firstCpf = cpf();
        String secondCpf = cpf();
        studentExists("2024002", firstCpf, "Bruno Henrique Lima");
        studentExists("2024002", secondCpf, "Bruno Henrique Lima");
        post("/auth/register/student", university("2024002", firstCpf, email()));

        HttpResponse<String> response = post("/auth/register/student", university("2024002", secondCpf, email()));

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(message(response)).isEqualTo("Registration number already registered");
    }

    @Test
    void registryDownReturns503AndCreatesNothing() throws Exception {
        when(universityRegistry.findActiveStudent(anyString(), anyString()))
                .thenThrow(new UniversityRegistryUnavailableException(new RuntimeException("down")));

        HttpResponse<String> response = post("/auth/register/student", university("2024001", cpf(), email()));

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(message(response)).isEqualTo("University registry unavailable");
        assertThat(userRepository.count()).isZero();
    }

    @Test
    void roleInBodyIsIgnored() throws Exception {
        String cpf = cpf();
        studentExists("2024003", cpf, "Carla Mendes Rocha");
        String body = """
                {"registrationNumber":"2024003","cpf":"%s","email":"%s","password":"%s","role":"LIBRARIAN","name":"Outro"}
                """.formatted(cpf, email(), PASSWORD);

        HttpResponse<String> response = post("/auth/register/student", body);

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode user = json.readTree(response.body());
        assertThat(user.get("role").asString()).isEqualTo("MEMBER");
        assertThat(user.get("name").asString()).isEqualTo("Carla Mendes Rocha");
    }

    @Test
    void counterStudentActivatesTheExistingAccount() throws Exception {
        String cpf = cpf();
        String email = email();
        long counterId = json.readTree(post("/users", """
                {"name":"Nome digitado no balcao","email":"%s","cpf":"%s","type":"STUDENT"}
                """.formatted(email, cpf)).body()).get("id").asLong();
        studentExists("2024001", cpf, "Ana Beatriz Souza");

        HttpResponse<String> response = post("/auth/register/student", university("2024001", cpf, email));

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(json.readTree(response.body()).get("id").asLong()).isEqualTo(counterId);
        User saved = userRepository.findById(counterId).orElseThrow();
        assertThat(saved.getName()).isEqualTo("Ana Beatriz Souza");
        assertThat(saved.getRegistrationNumber()).isEqualTo("2024001");
        assertThat(login(email).statusCode()).isEqualTo(200);
    }

    // ---------- login ----------

    @Test
    void loginIsRefusedWhenTheAffiliationEnds() throws Exception {
        String cpf = cpf();
        String email = email();
        studentExists("2024001", cpf, "Ana Beatriz Souza");
        post("/auth/register/student", university("2024001", cpf, email));

        // the student graduated: the registry no longer finds an active record
        when(universityRegistry.findActiveStudent("2024001", cpf)).thenReturn(Optional.empty());
        HttpResponse<String> response = login(email);

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(message(response)).isEqualTo("University affiliation is no longer active");
        assertThat(response.body()).doesNotContain("token");
    }

    @Test
    void loginFailsWith503WhenTheRegistryIsDown() throws Exception {
        String cpf = cpf();
        String email = email();
        studentExists("2024001", cpf, "Ana Beatriz Souza");
        post("/auth/register/student", university("2024001", cpf, email));

        when(universityRegistry.findActiveStudent("2024001", cpf))
                .thenThrow(new UniversityRegistryUnavailableException(new RuntimeException("down")));
        HttpResponse<String> response = login(email);

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.body()).doesNotContain("token");
    }

    @Test
    void wrongPasswordIsCheckedBeforeTheUniversity() throws Exception {
        String cpf = cpf();
        String email = email();
        studentExists("2024001", cpf, "Ana Beatriz Souza");
        post("/auth/register/student", university("2024001", cpf, email));
        when(universityRegistry.findActiveStudent("2024001", cpf)).thenReturn(Optional.empty());

        HttpResponse<String> response = post("/auth/login", """
                {"email":"%s","password":"senha-errada"}""".formatted(email));

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void externalLoginDoesNotAskTheUniversity() throws Exception {
        String email = email();
        post("/auth/register/external", """
                {"name":"Externa","email":"%s","cpf":"%s","password":"%s"}""".formatted(email, cpf(), PASSWORD));
        when(universityRegistry.findActiveStudent(anyString(), anyString()))
                .thenThrow(new UniversityRegistryUnavailableException(new RuntimeException("down")));

        assertThat(login(email).statusCode()).isEqualTo(200);
    }

    // ---------- helpers ----------

    private void studentExists(String number, String cpf, String name) {
        when(universityRegistry.findActiveStudent(number, cpf))
                .thenReturn(Optional.of(new UniversityPerson(number, name, "Curso")));
    }

    private String message(HttpResponse<String> response) {
        return json.readTree(response.body()).get("message").asString();
    }

    private static String email() {
        return "uni" + SEQ.incrementAndGet() + "-" + System.nanoTime() + "@test.local";
    }

    private static String cpf() {
        return String.format("%011d", ThreadLocalRandom.current().nextLong(1, 99_999_999_999L));
    }

    private static String university(String number, String cpf, String email) {
        return """
                {"registrationNumber":"%s","cpf":"%s","email":"%s","password":"%s"}
                """.formatted(number, cpf, email, PASSWORD);
    }

    private HttpResponse<String> login(String email) throws Exception {
        return post("/auth/login", """
                {"email":"%s","password":"%s"}""".formatted(email, PASSWORD));
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
