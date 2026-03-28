package com.project.homeless_shelter_availability_api;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.containers.wait.strategy.Wait;

@Testcontainers
@SpringBootTest
class HomelessShelterAvailabilityApiApplicationTests {
	static final ImageFromDockerfile postgresImage = new ImageFromDockerfile("hsa-postgres-test:latest", false)
		.withDockerfile(Path.of("..", "homeless-shelter-availability-data", "Dockerfile"));

	@Container
	static GenericContainer<?> postgres_container = new GenericContainer<>(postgresImage)
		.withExposedPorts(5432)
		.waitingFor(Wait.forListeningPort());

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () ->
			"jdbc:postgresql://" + postgres_container.getHost() + ":" + postgres_container.getMappedPort(5432) + "/HSADB");
		registry.add("spring.datasource.username", () -> "postgres");
		registry.add("spring.datasource.password", () -> "ZkY6tG5lp!");
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
	}

	@Test
	void contextLoads() {
	}

}
