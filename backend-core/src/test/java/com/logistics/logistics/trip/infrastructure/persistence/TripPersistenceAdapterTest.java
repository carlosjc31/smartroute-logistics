package com.logistics.logistics.trip.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.logistics.logistics.shared.application.PageResult;
import com.logistics.logistics.shared.domain.Coordinates;
import com.logistics.logistics.trip.domain.EtaPrediction;
import com.logistics.logistics.trip.domain.EtaSource;
import com.logistics.logistics.trip.domain.Trip;
import com.logistics.logistics.trip.domain.TripStatus;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;

/** Sobe o contexto completo com PostgreSQL embarcado: valida Flyway + mapeamento JPA (ddl-auto=validate). */
@SpringBootTest
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
class TripPersistenceAdapterTest {

  @Autowired TripPersistenceAdapter adapter;

  // @req TRIP-005
  @Test
  void savesLoadsAndPagesTrips() {
    OffsetDateTime departure = OffsetDateTime.of(2030, 1, 1, 8, 0, 0, 0, ZoneOffset.ofHours(-3));
    Trip trip = Trip.create(new Coordinates(-23.55, -46.63), new Coordinates(-22.90, -43.17), "SP", "RJ", 10.0,
            departure, OffsetDateTime.now(ZoneOffset.UTC))
        .withPrediction(new EtaPrediction(5.25, 0.89, "v1", EtaSource.MODEL));

    adapter.save(trip);
    adapter.save(adapter.findById(trip.id()).orElseThrow().transitionTo(TripStatus.IN_TRANSIT, departure));

    Trip loaded = adapter.findById(trip.id()).orElseThrow();
    assertThat(loaded.status()).isEqualTo(TripStatus.IN_TRANSIT);
    assertThat(loaded.predictedArrival().toInstant()).isEqualTo(departure.plusMinutes(315).toInstant());
    assertThat(loaded.etaSource()).isEqualTo(EtaSource.MODEL);

    PageResult<Trip> inTransit = adapter.findAll(TripStatus.IN_TRANSIT, 0, 10);
    assertThat(inTransit.items()).extracting(Trip::id).contains(trip.id());
    assertThat(adapter.findAll(TripStatus.CANCELLED, 0, 10).items()).extracting(Trip::id).doesNotContain(trip.id());
  }
}
