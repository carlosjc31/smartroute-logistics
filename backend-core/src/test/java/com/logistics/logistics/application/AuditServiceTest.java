package com.logistics.logistics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.logistics.logistics.application.audit.AuditAction;
import com.logistics.logistics.application.audit.AuditEntry;
import com.logistics.logistics.application.port.out.AuditLogPort;
import com.logistics.logistics.application.service.AuditService;
import com.logistics.logistics.domain.Coordinates;
import com.logistics.logistics.domain.EtaPrediction;
import com.logistics.logistics.domain.EtaSource;
import com.logistics.logistics.domain.Trip;
import com.logistics.logistics.domain.TripStatus;

class AuditServiceTest {

  private static final Instant NOW = Instant.parse("2030-01-01T10:00:00Z");
  private static final Trip TRIP = Trip.create(new Coordinates(-23.55, -46.63), new Coordinates(-22.90, -43.17),
          "Rua das Flores, 10", "Av. Atlântica, 500", 12.0,
          OffsetDateTime.of(2030, 1, 2, 8, 0, 0, 0, ZoneOffset.ofHours(-3)), OffsetDateTime.now(ZoneOffset.UTC))
      .withPrediction(new EtaPrediction(5.0, 0.89, "v1", EtaSource.MODEL));

  private final InMemoryAuditLog log = new InMemoryAuditLog();
  private final AuditService service = new AuditService(log, () -> "bruno.admin", Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void recordsActorTimestampAndStates() {
    Trip inTransit = TRIP.transitionTo(TripStatus.IN_TRANSIT, OffsetDateTime.now());

    service.recordTripChange(AuditAction.TRIP_STATUS_CHANGED, TRIP, inTransit);

    AuditEntry entry = log.entries.get(0);
    assertThat(entry.actor()).isEqualTo("bruno.admin");
    assertThat(entry.occurredAt().toInstant()).isEqualTo(NOW);
    assertThat(entry.entityType()).isEqualTo(AuditService.TRIP);
    assertThat(entry.entityId()).isEqualTo(TRIP.id());
    assertThat(entry.before()).containsEntry("status", "PENDING").containsEntry("etaSource", "MODEL");
    assertThat(entry.after()).containsEntry("status", "IN_TRANSIT").containsEntry("payloadWeight", 12.0);
  }

  @Test
  void snapshotNeverContainsCoordinatesOrAddresses() {
    service.recordTripChange(AuditAction.TRIP_CREATED, null, TRIP);

    Map<String, Object> state = log.entries.get(0).after();
    assertThat(state).doesNotContainKeys("origin", "destination", "originLabel", "destinationLabel");
    assertThat(state.values()).noneMatch(value -> String.valueOf(value).contains("Rua das Flores"));
    assertThat(log.entries.get(0).before()).isNull();
  }

  @Test
  void returnsHistoryFromPort() {
    service.recordTripChange(AuditAction.TRIP_CREATED, null, TRIP);

    PageResult<AuditEntry> history = service.tripHistory(TRIP.id(), 0, 10);

    assertThat(history.items()).hasSize(1);
    assertThat(service.tripHistory(UUID.randomUUID(), 0, 10).items()).isEmpty();
  }

  @Test
  void entryRequiresMandatoryFields() {
    assertThatThrownBy(() -> new AuditEntry(UUID.randomUUID(), null, "x", AuditAction.TRIP_CREATED, "TRIP",
        UUID.randomUUID(), null, Map.of())).isInstanceOf(IllegalArgumentException.class);
  }

  public static class InMemoryAuditLog implements AuditLogPort {
    public final List<AuditEntry> entries = new ArrayList<>();

    @Override
    public void append(AuditEntry entry) {
      entries.add(entry);
    }

    @Override
    public PageResult<AuditEntry> findByEntity(String entityType, UUID entityId, int page, int size) {
      List<AuditEntry> matching = entries.stream()
          .filter(e -> e.entityType().equals(entityType) && e.entityId().equals(entityId))
          .sorted(Comparator.comparing(AuditEntry::occurredAt).reversed())
          .toList();
      return new PageResult<>(matching, page, size, matching.size(), matching.isEmpty() ? 0 : 1);
    }
  }
}
