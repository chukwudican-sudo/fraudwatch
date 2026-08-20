package com.fraudwatch.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. Spring Boot scans this package (and sub-packages) for
 * annotated classes like @RestController and wires everything together
 * at startup — this is the "convention over configuration" part that
 * mirrors how FastAPI auto-wires routes from decorators.
 */
@SpringBootApplication
public class FraudwatchBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraudwatchBackendApplication.class, args);
    }
}
