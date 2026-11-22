package com.logistics.logistics.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TripTest {

  private static final Coordinates SAO_PAULO = new Coordinates(-23.5505, -46.6333);
  private static final Coordinates RIO = new Coordinates(-22.9068, -43.1729);
  private static final LocalDateTime DEPARTURE = LocalDateTime.of(2030, 1, 1, 8, 0);

  private Trip trip(Coordinates origin, Coordinates destination, Double weight) {
    return new Trip(UUID.randomUUID(), origin, destination, weight, DEPARTURE, Trip.TripStatus.PENDING);
  }

  @Test
  void distanceBetweenSaoPauloAndRioIsAbout360Km() {
    assertThat(trip(SAO_PAULO, RIO, 10.0).distanceKm()).isCloseTo(361, within(5.0));
  }

  @Test
  void distanceIsFiniteForVeryClosePoints() {
    Coordinates nearby = new Coordinates(-23.5505, -46.63330001);
    assertThat(SAO_PAULO.distanceKmTo(nearby)).isFinite().isGreaterThanOrEqualTo(0);
  }

  @Test
  void estimateArrivalKeepsMinutes() {
    assertThat(trip(SAO_PAULO, RIO, 10.0).estimateArrival(2.9))
        .isEqualTo(DEPARTURE.plusHours(2).plusMinutes(54));
  }

  @Test
  void rejectsNullWeight() {
    assertThatThrownBy(() -> trip(SAO_PAULO, RIO, null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsSameOriginAndDestination() {
    assertThatThrownBy(() -> trip(SAO_PAULO, SAO_PAULO, 10.0)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsInvalidLatitude() {
    assertThatThrownBy(() -> new Coordinates(91.0, 0.0)).isInstanceOf(IllegalArgumentException.class);
  }
}
