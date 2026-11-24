package com.library.university_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "eureka.client.enabled=false")
class VerificationTests {

    @Container
    @ServiceConnection
    static MariaDBContainer mariadb = new MariaDBContainer("mariadb:11");

    @LocalServerPort
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void activeStudentWithMatchingCpfIsFound() throws Exception {
        HttpResponse<String> response = verify("/students/verify", "2024001", "11111111111");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"registrationNumber\":\"2024001\"")
                .contains("Ana Beatriz Souza")
                .contains("Ciência da Computação")
                .doesNotContain("11111111111");
    }

    @Test
    void activeTeacherWithMatchingCpfIsFound() throws Exception {
        HttpResponse<String> response = verify("/teachers/verify", "10203040", "55555555555");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Eduardo Castro Nunes").contains("Departamento de Computação");
    }

    @Test
    void wrongCpfInactiveAndUnknownGetTheSame404() throws Exception {
        HttpResponse<String> wrongCpf = verify("/students/verify", "2024001", "99999999999");
        HttpResponse<String> inactive = verify("/students/verify", "2019050", "44444444444");
        HttpResponse<String> unknown = verify("/students/verify", "0000000", "11111111111");
        HttpResponse<String> inactiveTeacher = verify("/teachers/verify", "90807060", "77777777777");

        assertThat(wrongCpf.statusCode()).isEqualTo(404);
        assertThat(inactive.statusCode()).isEqualTo(404);
        assertThat(unknown.statusCode()).isEqualTo(404);
        assertThat(inactiveTeacher.statusCode()).isEqualTo(404);
        assertThat(wrongCpf.body()).isEqualTo(inactive.body()).isEqualTo(unknown.body());
    }

    @Test
    void studentCannotBeVerifiedAsTeacher() throws Exception {
        // separate registries: a student's number is not in the teacher base
        assertThat(verify("/teachers/verify", "2024001", "11111111111").statusCode()).isEqualTo(404);
        assertThat(verify("/students/verify", "10203040", "55555555555").statusCode()).isEqualTo(404);
    }

    @Test
    void invalidCpfFormatIsRejected() throws Exception {
        HttpResponse<String> response = verify("/students/verify", "2024001", "123");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("cpf: CPF must have 11 digits");
    }

    private HttpResponse<String> verify(String path, String number, String cpf) throws Exception {
        String body = "{\"registrationNumber\":\"" + number + "\",\"cpf\":\"" + cpf + "\"}";
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
