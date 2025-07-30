package com.onlyoffice.slack.integration;

import java.time.Duration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Transactional
@SpringBootTest
@Testcontainers
@ActiveProfiles("integration")
public abstract class BaseIntegrationTest {

  @Container
  protected static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("integration_test_db")
          .withUsername("test_user")
          .withPassword("test_password")
          .withReuse(true)
          .withStartupTimeout(Duration.ofMinutes(2))
          .withConnectTimeoutSeconds(60);

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);

    registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
    registry.add("spring.datasource.hikari.minimum-idle", () -> "1");
    registry.add("spring.datasource.hikari.connection-timeout", () -> "10000");
    registry.add("spring.datasource.hikari.idle-timeout", () -> "30000");
    registry.add("spring.datasource.hikari.max-lifetime", () -> "60000");
    registry.add("spring.datasource.hikari.leak-detection-threshold", () -> "30000");

    registry.add("hazelcast.enabled", () -> "false");
    registry.add("spring.cache.type", () -> "simple");
  }
}
