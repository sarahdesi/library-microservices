package com.library.loan_service.client;

import com.library.loan_service.exception.ServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A service missing from Eureka must become a 503, not a 500 */
class ClientUnavailableTests {

    private final RestClient noInstances = RestClient.builder()
            .requestInterceptor((request, body, execution) -> {
                throw new IllegalStateException("No instances available");
            })
            .build();

    @Test
    void catalogNotRegistered() {
        assertThatThrownBy(() -> new BookClient(noInstances).changeAvailability("b1", -1))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Catalog service unavailable");
    }

    @Test
    void userServiceNotRegistered() {
        assertThatThrownBy(() -> new UserClient(noInstances).getUser(1L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("User service unavailable");
    }
}
