package com.project.homeless_shelter_availability_api.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class PostgresContainerSupport {

    @Container
    private static final JdbcDatabaseContainer<?> POSTGRES = postgresContainer();

    private static JdbcDatabaseContainer<?> postgresContainer() {
        JdbcDatabaseContainer<?> postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:15"));
        postgres.withDatabaseName("HSADB");
        postgres.withUsername("postgres");
        postgres.withPassword("postgres");
        return postgres;
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
