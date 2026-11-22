package com.logistics.logistics;

import java.io.IOException;

import javax.sql.DataSource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * Sobe o backend localmente com um PostgreSQL embarcado, sem Docker:
 * {@code ./mvnw spring-boot:test-run}. Os dados somem ao encerrar.
 */
public class TestBackendCoreApplication {

  public static void main(String[] args) {
    SpringApplication.from(BackendCoreApplication::main).with(EmbeddedPostgresConfig.class).run(args);
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class EmbeddedPostgresConfig {
    @Bean(destroyMethod = "close")
    EmbeddedPostgres embeddedPostgres() throws IOException {
      return EmbeddedPostgres.start();
    }

    @Bean
    DataSource dataSource(EmbeddedPostgres postgres) {
      return postgres.getPostgresDatabase();
    }
  }
}
