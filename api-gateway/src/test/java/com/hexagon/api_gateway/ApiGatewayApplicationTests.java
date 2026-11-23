package com.hexagon.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.jwt.secret=gateway-test-secret-with-at-least-32-chars")
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
