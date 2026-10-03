package com.prijilevschi;

import com.prijilevschi.config.SqliteDirectoryInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LibraryAppApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(LibraryAppApplication.class);
		app.addListeners(new SqliteDirectoryInitializer());
		app.run(args);
	}

}
