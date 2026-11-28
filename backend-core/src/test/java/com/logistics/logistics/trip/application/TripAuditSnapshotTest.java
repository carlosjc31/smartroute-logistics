package com.logistics.logistics.trip.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.logistics.logistics.shared.domain.Coordinates;
import com.logistics.logistics.trip.domain.EtaPrediction;
import com.logistics.logistics.trip.domain.EtaSource;
import com.logistics.logistics.trip.domain.Trip;
import com.logistics.logistics.trip.domain.TripStatus;

class TripAuditSnapshotTest {

  private static final Trip TRIP = Trip.create(new Coordinates(-23.55, -46.63), new Coordinates(-22.90, -43.17),
          "Rua das Flores, 10", "Av. Atlântica, 500", 12.0,
          OffsetDateTime.of(2030, 1, 2, 8, 0, 0, 0, ZoneOffset.ofHours(-3)), OffsetDateTime.now(ZoneOffset.UTC))
      .withPrediction(new EtaPrediction(5.0, 0.89, "v1", EtaSource.MODEL));

  // @req TRIP-010
  @Test
  void keepsOperationalFields() {
    Map<String, Object> state = TripAuditSnapshot.of(TRIP.transitionTo(TripStatus.IN_TRANSIT, OffsetDateTime.now()));

    assertThat(state).containsEntry("status", "IN_TRANSIT").containsEntry("payloadWeight", 12.0)
        .containsEntry("etaSource", "MODEL").containsEntry("etaModelVersion", "v1");
  }

  // @req TRIP-011
  @Test
  void neverContainsCoordinatesOrAddresses() {
    Map<String, Object> state = TripAuditSnapshot.of(TRIP);

    assertThat(state).doesNotContainKeys("origin", "destination", "originLabel", "destinationLabel");
    assertThat(state.values()).noneMatch(value -> String.valueOf(value).contains("Rua das Flores")
        || String.valueOf(value).contains("-23.55"));
  }
}
