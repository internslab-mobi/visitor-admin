package com.adminvisitor;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import static org.junit.jupiter.api.Assertions.assertTrue;
@EnableJpaAuditing
class VmsApplicationTests {

	@Test
	void applicationTest_shouldPass() {
		assertTrue(true);
	}
}