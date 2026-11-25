package com.prijilevschi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "library.api-token=context-test-token-123")
class LibraryAppApplicationTests extends SqliteTestSupport {

	@Test
	void contextLoads() {
	}

}
