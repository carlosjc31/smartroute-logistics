package com.logistics.logistics.trip.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * Bancos criados antes do Flyway (Hibernate com ddl-auto=update) precisam migrar sem perder dados.
 * Recria o schema legado, insere uma viagem e aplica as migrations com a mesma configuração de baseline.
 */
class LegacySchemaMigrationTest {

  private static final String LEGACY_SCHEMA = """
      create table trips (
        id uuid not null, created_at timestamp(6), departure_time timestamp(6),
        dest_lat float(53), dest_lng float(53), origin_lat float(53), origin_lng float(53),
        payload_weight float(53), predicted_arrival timestamp(6),
        status varchar(255) check (status in ('PENDING','IN_TRANSIT','COMPLETED','DELAYED')),
        primary key (id))
      """;

  @Test
  void migratesHibernateCreatedSchemaKeepingData() throws Exception {
    try (EmbeddedPostgres postgres = EmbeddedPostgres.start()) {
      DataSource dataSource = postgres.getPostgresDatabase();
      try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
        s.execute(LEGACY_SCHEMA);
        s.execute("""
            insert into trips values ('00000000-0000-0000-0000-000000000001', '2026-03-01 10:00:00',
              '2026-03-02 08:00:00', -22.9, -43.17, -23.55, -46.63, 10, '2026-03-02 13:00:00', 'PENDING')
            """);
      }

      Flyway.configure().dataSource(dataSource).baselineOnMigrate(true).baselineVersion("0").load().migrate();

      try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
        // V4 move a tabela para o schema do módulo (trip), com os dados
        ResultSet rs = s.executeQuery("select status, eta_source, predicted_arrival from trip.trips");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("status")).isEqualTo("PENDING");
        assertThat(rs.getString("eta_source")).isEqualTo("MODEL");
        assertThat(rs.getObject("predicted_arrival", java.time.OffsetDateTime.class).toInstant())
            .hasToString("2026-03-02T13:00:00Z");
        s.execute("update trip.trips set status = 'CANCELLED'"); // novo status aceito pelo CHECK
      }
    }
  }
}
