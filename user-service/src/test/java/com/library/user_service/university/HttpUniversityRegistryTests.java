package com.library.user_service.university;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpUniversityRegistryTests {

    @Test
    void foundStudentIsMappedAndCpfGoesInTheBody() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://UNIVERSITY-SERVICE/students/verify"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"registrationNumber\":\"2024001\",\"cpf\":\"11111111111\"}"))
                .andRespond(withSuccess("{\"registrationNumber\":\"2024001\",\"name\":\"Ana\",\"course\":\"Computação\"}",
                        MediaType.APPLICATION_JSON));

        var person = new HttpUniversityRegistry(builder.build()).findActiveStudent("2024001", "11111111111");

        assertThat(person).contains(new UniversityPerson("2024001", "Ana", "Computação"));
        server.verify();
    }

    @Test
    void notFoundMeansNoActiveRecord() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://UNIVERSITY-SERVICE/teachers/verify")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(new HttpUniversityRegistry(builder.build()).findActiveTeacher("1", "11111111111")).isEmpty();
    }

    @Test
    void serverErrorMeansUnavailable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://UNIVERSITY-SERVICE/students/verify")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> new HttpUniversityRegistry(builder.build()).findActiveStudent("1", "11111111111"))
                .isInstanceOf(UniversityRegistryUnavailableException.class);
    }

    @Test
    void noInstanceInEurekaMeansUnavailable() {
        // what the load balancer throws when the service is not registered
        RestClient client = RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    throw new IllegalStateException("No instances available for UNIVERSITY-SERVICE");
                })
                .build();

        assertThatThrownBy(() -> new HttpUniversityRegistry(client).findActiveStudent("1", "11111111111"))
                .isInstanceOf(UniversityRegistryUnavailableException.class)
                .hasMessage("University registry unavailable");
    }
}
