package com.library.loan_service.client;

import com.library.loan_service.dto.UserData;
import com.library.loan_service.exception.ResourceNotFoundException;
import com.library.loan_service.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class UserClient {

    private final RestClient restClient;

    public UserData getUser(Long userId) {
        try {
            return restClient.get()
                    .uri("http://USER-SERVICE/users/{id}", userId)
                    .retrieve()
                    .body(UserData.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("User not found");
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("User service unavailable");
        }
    }
}