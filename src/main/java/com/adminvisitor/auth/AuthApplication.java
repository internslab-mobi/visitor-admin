package com.adminvisitor.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application entry point for the Visitor Management System Authentication and Authorization Service.
 * <p>
 * Bootstraps the Spring Boot application context, initializing security filters, database repositories,
 * asynchronous task executors, and web REST endpoints.
 * </p>
 *
 * @author Logeshwaran
 */
@SpringBootApplication
public class AuthApplication {

	/**
	 * Launches the Authentication Service application.
	 *
	 * @param args command-line arguments passed to the application
	 */
	public static void main(String[] args) {
		SpringApplication.run(AuthApplication.class, args);
	}

}
