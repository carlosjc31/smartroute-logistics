package com.logistics.logistics.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.logistics.logistics.shared.application.PageResult;

class AuditServiceTest {

  private static final Instant NOW = Instant.parse("2030-01-01T10:00:00Z");
  private static final UUID TRIP_ID = UUID.randomUUID();

  private final InMemoryAuditLog log = new InMemoryAuditLog();
  private final AuditService service = new AuditService(log, () -> "bruno.admin", Clock.fixed(NOW, ZoneOffset.UTC));

  // @req TRIP-010
  @Test
  void recordsActorTimestampAndStates() {
    service.record(AuditAction.TRIP_STATUS_CHANGED, AuditService.TRIP, TRIP_ID, Map.of("status", "PENDING"),
        Map.of("status", "IN_TRANSIT"));

    AuditEntry entry = log.entries.get(0);
    assertThat(entry.actor()).isEqualTo("bruno.admin");
    assertThat(entry.occurredAt().toInstant()).isEqualTo(NOW);
    assertThat(entry.action()).isEqualTo(AuditAction.TRIP_STATUS_CHANGED);
    assertThat(entry.entityType()).isEqualTo(AuditService.TRIP);
    assertThat(entry.entityId()).isEqualTo(TRIP_ID);
    assertThat(entry.before()).containsEntry("status", "PENDING");
    assertThat(entry.after()).containsEntry("status", "IN_TRANSIT");
  }

  // @req TRIP-013
  @Test
  void returnsHistoryFromPort() {
    service.record(AuditAction.TRIP_CREATED, AuditService.TRIP, TRIP_ID, null, Map.of("status", "PENDING"));

    PageResult<AuditEntry> history = service.tripHistory(TRIP_ID, 0, 10);

    assertThat(history.items()).hasSize(1);
    assertThat(history.items().get(0).before()).isNull();
    assertThat(service.tripHistory(UUID.randomUUID(), 0, 10).items()).isEmpty();
  }

  // @req TRIP-010
  @Test
  void entryRequiresMandatoryFields() {
    assertThatThrownBy(() -> new AuditEntry(UUID.randomUUID(), null, "x", AuditAction.TRIP_CREATED, "TRIP",
        UUID.randomUUID(), null, Map.of())).isInstanceOf(IllegalArgumentException.class);
  }
}
