package com.srihari.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Execution(ExecutionMode.SAME_THREAD) // Spring Boot tests should run in same thread for context sharing
class AiFundamentalsApplicationTests {

	@Test
	void contextLoads() {
	}

}
