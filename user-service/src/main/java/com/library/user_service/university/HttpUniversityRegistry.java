package com.library.user_service.university;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;

/** Calls the university-service through Eureka (http://UNIVERSITY-SERVICE). The CPF goes in the body. */
@Component
@RequiredArgsConstructor
public class HttpUniversityRegistry implements UniversityRegistry {

    private static final String BASE_URL = "http://UNIVERSITY-SERVICE";

    private final RestClient restClient;

    private record StudentBody(String registrationNumber, String name, String course) {}

    private record TeacherBody(String registrationNumber, String name, String department) {}

    @Override
    public Optional<UniversityPerson> findActiveStudent(String registrationNumber, String cpf) {
        return verify("/students/verify", registrationNumber, cpf, StudentBody.class)
                .map(body -> new UniversityPerson(body.registrationNumber(), body.name(), body.course()));
    }

    @Override
    public Optional<UniversityPerson> findActiveTeacher(String masp, String cpf) {
        return verify("/teachers/verify", masp, cpf, TeacherBody.class)
                .map(body -> new UniversityPerson(body.registrationNumber(), body.name(), body.department()));
    }

    private <T> Optional<T> verify(String path, String number, String cpf, Class<T> type) {
        try {
            return Optional.ofNullable(restClient.post()
                    .uri(BASE_URL + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("registrationNumber", number, "cpf", cpf))
                    .retrieve()
                    .body(type));
        } catch (HttpClientErrorException.NotFound e) {
            // unknown, inactive or CPF that does not match: the registry does not say which
            return Optional.empty();
        } catch (RestClientException e) {
            throw new UniversityRegistryUnavailableException(e);
        }
    }
}
