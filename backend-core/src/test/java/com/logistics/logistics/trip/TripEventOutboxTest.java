package com.logistics.logistics.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;

import com.logistics.logistics.shared.domain.Coordinates;
import com.logistics.logistics.trip.application.CreateTripCommand;
import com.logistics.logistics.trip.application.EtaPredictionPort;
import com.logistics.logistics.trip.application.EtaPredictionUnavailableException;
import com.logistics.logistics.trip.application.TripService;
import com.logistics.logistics.trip.domain.Trip;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;

/**
 * Outbox (ADR 0004): o evento publicado na transação da viagem fica em events.event_publication e é entregue
 * ao ouvinte do outro módulo depois do commit. O ouvinte daqui faz o papel de um consumidor futuro (#43).
 */
@SpringBootTest
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
@Import(TripEventOutboxTest.RecordingListener.class)
class TripEventOutboxTest {

  @MockBean EtaPredictionPort eta;
  @Autowired TripService trips;
  @Autowired JdbcTemplate jdbc;
  @Autowired RecordingListener listener;

  // @req TRIP-014
  @Test
  void eventIsStoredInTheOutboxAndDeliveredAfterCommit() {
    when(eta.predict(any())).thenThrow(new EtaPredictionUnavailableException("ai-engine fora", null));

    Trip trip = trips.createTrip(new CreateTripCommand(new Coordinates(-23.55, -46.63),
        new Coordinates(-22.90, -43.17), "Rua das Flores, 10", "Av. Atlântica, 500", 8.0,
        OffsetDateTime.of(2030, 1, 2, 8, 0, 0, 0, ZoneOffset.ofHours(-3))));

    await().atMost(Duration.ofSeconds(10)).until(() -> listener.received().contains(trip.id()));
    await().atMost(Duration.ofSeconds(10)).until(() -> completedPublications(trip.id()) == 1);

    String stored = jdbc.queryForObject(
        "select serialized_event from events.event_publication where serialized_event like ?", String.class,
        "%" + trip.id() + "%");
    assertThat(stored).doesNotContain("Rua das Flores", "Atlântica", "-23.55", "-46.63");
  }

  private int completedPublications(UUID tripId) {
    return jdbc.queryForObject("""
        select count(*) from events.event_publication
        where completion_date is not null and serialized_event like ?
        """, Integer.class, "%" + tripId + "%");
  }

  @TestComponent
  static class RecordingListener {
    private final List<UUID> received = new CopyOnWriteArrayList<>();

    @ApplicationModuleListener
    void on(TripCreated event) {
      received.add(event.tripId());
    }

    /** Método, não campo: o bean é um proxy (@Async), e o campo do proxy fica vazio. */
    public List<UUID> received() {
      return received;
    }
  }
}
