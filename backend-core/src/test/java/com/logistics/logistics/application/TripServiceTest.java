package com.logistics.logistics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.logistics.logistics.application.audit.AuditAction;
import com.logistics.logistics.application.audit.AuditEntry;
import com.logistics.logistics.application.exception.EtaPredictionUnavailableException;
import com.logistics.logistics.application.service.AuditService;
import com.logistics.logistics.application.exception.TripNotFoundException;
import com.logistics.logistics.application.port.out.EtaPredictionPort;
import com.logistics.logistics.application.port.out.TripRepositoryPort;
import com.logistics.logistics.application.service.CreateTripCommand;
import com.logistics.logistics.application.service.TripService;
import com.logistics.logistics.domain.Coordinates;
import com.logistics.logistics.domain.EtaPrediction;
import com.logistics.logistics.domain.EtaSource;
import com.logistics.logistics.domain.HeuristicEtaEstimator;
import com.logistics.logistics.domain.Trip;
import com.logistics.logistics.domain.TripStatus;
import com.logistics.logistics.domain.exception.InvalidStatusTransitionException;

class TripServiceTest {

  private static final Instant NOW = Instant.parse("2030-01-01T10:00:00Z");
  private static final OffsetDateTime DEPARTURE = OffsetDateTime.of(2030, 1, 2, 14, 0, 0, 0, ZoneOffset.ofHours(-3));

  private final InMemoryTrips repository = new InMemoryTrips();
  private final StubEta eta = new StubEta();
  private final AuditServiceTest.InMemoryAuditLog auditLog = new AuditServiceTest.InMemoryAuditLog();
  private TripService service;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    service = new TripService(repository, eta, new AuditService(auditLog, () -> "ana.despacho", clock), clock);
  }

  private CreateTripCommand command() {
    return new CreateTripCommand(new Coordinates(-23.55, -46.63), new Coordinates(-22.90, -43.17),
        "São Paulo", "Rio de Janeiro", 12.0, DEPARTURE);
  }

  // @req TRIP-003
  @Test
  void createsTripWithModelPrediction() {
    eta.next = new EtaPrediction(5.5, 0.89, "v1.0-random-forest", EtaSource.MODEL);

    Trip trip = service.createTrip(command());

    assertThat(trip.predictedArrival()).isEqualTo(DEPARTURE.plusMinutes(330));
    assertThat(trip.etaSource()).isEqualTo(EtaSource.MODEL);
    assertThat(trip.createdAt().toInstant()).isEqualTo(NOW);
    assertThat(repository.findById(trip.id())).isPresent();
  }

  // @req TRIP-004
  @Test
  void fallsBackToHeuristicWhenAiEngineIsDown() {
    eta.failure = new EtaPredictionUnavailableException("offline", null);

    Trip trip = service.createTrip(command());

    assertThat(trip.etaSource()).isEqualTo(EtaSource.FALLBACK);
    assertThat(trip.etaModelVersion()).isEqualTo(HeuristicEtaEstimator.VERSION);
    assertThat(trip.predictedArrival()).isAfter(DEPARTURE);
  }

  // @req TRIP-008
  @Test
  void updatesStatusAndRecordsArrival() {
    eta.next = new EtaPrediction(5.0, null, "v1", EtaSource.MODEL);
    Trip trip = service.createTrip(command());

    service.updateStatus(trip.id(), TripStatus.IN_TRANSIT);
    Trip done = service.updateStatus(trip.id(), TripStatus.COMPLETED);

    assertThat(done.status()).isEqualTo(TripStatus.COMPLETED);
    assertThat(done.actualArrival().toInstant()).isEqualTo(NOW);
  }

  // @req TRIP-010
  @Test
  void auditsCreationAndEveryStatusChange() {
    eta.next = new EtaPrediction(5.0, null, "v1", EtaSource.MODEL);
    Trip trip = service.createTrip(command());
    service.updateStatus(trip.id(), TripStatus.IN_TRANSIT);

    List<AuditEntry> history = auditLog.entries;
    assertThat(history).extracting(AuditEntry::action)
        .containsExactly(AuditAction.TRIP_CREATED, AuditAction.TRIP_STATUS_CHANGED);
    assertThat(history).allSatisfy(entry -> {
      assertThat(entry.actor()).isEqualTo("ana.despacho");
      assertThat(entry.entityId()).isEqualTo(trip.id());
    });
    assertThat(history.get(0).before()).isNull();
    assertThat(history.get(1).before()).containsEntry("status", "PENDING");
    assertThat(history.get(1).after()).containsEntry("status", "IN_TRANSIT");
  }

  // @req TRIP-010
  @Test
  void rejectedTransitionIsNotAudited() {
    eta.next = new EtaPrediction(5.0, null, "v1", EtaSource.MODEL);
    UUID id = service.createTrip(command()).id();

    assertThatThrownBy(() -> service.updateStatus(id, TripStatus.COMPLETED));
    assertThat(auditLog.entries).hasSize(1);
  }

  // @req TRIP-007
  @Test
  void rejectsInvalidTransition() {
    eta.next = new EtaPrediction(5.0, null, "v1", EtaSource.MODEL);
    UUID id = service.createTrip(command()).id();

    assertThatThrownBy(() -> service.updateStatus(id, TripStatus.COMPLETED))
        .isInstanceOf(InvalidStatusTransitionException.class);
  }

  // @req TRIP-006
  @Test
  void unknownTripThrowsNotFound() {
    assertThatThrownBy(() -> service.getTrip(UUID.randomUUID())).isInstanceOf(TripNotFoundException.class);
  }

  static class StubEta implements EtaPredictionPort {
    EtaPrediction next;
    RuntimeException failure;

    @Override
    public EtaPrediction predict(Trip trip) {
      if (failure != null)
        throw failure;
      return next;
    }
  }

  static class InMemoryTrips implements TripRepositoryPort {
    private final Map<UUID, Trip> trips = new HashMap<>();

    @Override
    public Trip save(Trip trip) {
      trips.put(trip.id(), trip);
      return trip;
    }

    @Override
    public Optional<Trip> findById(UUID id) {
      return Optional.ofNullable(trips.get(id));
    }

    @Override
    public PageResult<Trip> findAll(TripStatus status, int page, int size) {
      List<Trip> all = new ArrayList<>(trips.values().stream()
          .filter(t -> status == null || t.status() == status)
          .sorted(Comparator.comparing(Trip::createdAt).reversed())
          .toList());
      int from = Math.min(page * size, all.size());
      int to = Math.min(from + size, all.size());
      return new PageResult<>(all.subList(from, to), page, size, all.size(), (all.size() + size - 1) / size);
    }
  }
}
