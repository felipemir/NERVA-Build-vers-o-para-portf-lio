package com.nervatech.nerva_build;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class PostgresIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRESQL =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("nerva_test")
                    .withUsername("nerva_test")
                    .withPassword("nerva_test");

    static {
        POSTGRESQL.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
        registry.add(
                "app.jwt.secret",
                () -> "integration-test-secret-0123456789-ABCDEFGHIJKLMNOPQRSTUVWXYZ-abcdefghijk"
        );
        registry.add("app.onboarding.public-enabled", () -> "false");
    }
}
