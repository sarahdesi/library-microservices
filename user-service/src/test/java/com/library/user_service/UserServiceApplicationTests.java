package com.library.user_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.jwt.secret=test-secret-with-at-least-32-characters!!")
class UserServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
