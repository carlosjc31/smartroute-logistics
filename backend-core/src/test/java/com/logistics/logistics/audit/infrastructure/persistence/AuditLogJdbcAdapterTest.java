package com.logistics.logistics.audit.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.logistics.logistics.audit.AuditAction;
import com.logistics.logistics.audit.AuditEntry;
import com.logistics.logistics.shared.application.PageResult;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
class AuditLogJdbcAdapterTest {

  @Autowired AuditLogJdbcAdapter adapter;
  @Autowired JdbcTemplate jdbc;

  // @req TRIP-013
  @Test
  void appendsAndReadsHistoryNewestFirst() {
    UUID tripId = UUID.randomUUID();
    OffsetDateTime t0 = OffsetDateTime.of(2030, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
    adapter.append(entry(tripId, t0, AuditAction.TRIP_CREATED, null, Map.of("status", "PENDING")));
    adapter.append(entry(tripId, t0.plusMinutes(5), AuditAction.TRIP_STATUS_CHANGED,
        Map.of("status", "PENDING"), Map.of("status", "IN_TRANSIT", "payloadWeight", 12.5)));
    adapter.append(entry(UUID.randomUUID(), t0, AuditAction.TRIP_CREATED, null, Map.of("status", "PENDING")));

    PageResult<AuditEntry> history = adapter.findByEntity("TRIP", tripId, 0, 10);

    assertThat(history.totalItems()).isEqualTo(2);
    assertThat(history.totalPages()).isEqualTo(1);
    AuditEntry latest = history.items().get(0);
    assertThat(latest.action()).isEqualTo(AuditAction.TRIP_STATUS_CHANGED);
    assertThat(latest.occurredAt().toInstant()).isEqualTo(t0.plusMinutes(5).toInstant());
    assertThat(latest.actor()).isEqualTo("ana.despacho");
    assertThat(latest.before()).isEqualTo(Map.of("status", "PENDING"));
    assertThat(latest.after()).isEqualTo(Map.of("status", "IN_TRANSIT", "payloadWeight", 12.5));
    assertThat(history.items().get(1).before()).isNull();
    assertThat(adapter.findByEntity("TRIP", tripId, 1, 1).items()).extracting(AuditEntry::action)
        .containsExactly(AuditAction.TRIP_CREATED);
  }

  // @req TRIP-012
  @Test
  void databaseRejectsUpdateAndDelete() {
    UUID tripId = UUID.randomUUID();
    adapter.append(entry(tripId, OffsetDateTime.now(ZoneOffset.UTC), AuditAction.TRIP_CREATED, null,
        Map.of("status", "PENDING")));

    assertThatThrownBy(() -> jdbc.update("UPDATE audit.audit_log SET actor = 'mallory' WHERE entity_id = ?", tripId))
        .isInstanceOf(DataAccessException.class).hasMessageContaining("somente inserção");
    assertThatThrownBy(() -> jdbc.update("DELETE FROM audit.audit_log WHERE entity_id = ?", tripId))
        .isInstanceOf(DataAccessException.class).hasMessageContaining("somente inserção");
    assertThatThrownBy(() -> jdbc.execute("TRUNCATE audit.audit_log"))
        .isInstanceOf(DataAccessException.class).hasMessageContaining("somente inserção");
    assertThat(adapter.findByEntity("TRIP", tripId, 0, 10).items()).singleElement()
        .extracting(AuditEntry::actor).isEqualTo("ana.despacho");
  }

  private static AuditEntry entry(UUID tripId, OffsetDateTime at, AuditAction action, Map<String, Object> before,
      Map<String, Object> after) {
    return new AuditEntry(UUID.randomUUID(), at, "ana.despacho", action, "TRIP", tripId, before, after);
  }
}
