package com.logistics.logistics.trip.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class TripStatusTest {

  // @req TRIP-007
  @ParameterizedTest(name = "{0} -> {1} permitido = {2}")
  @CsvSource({
      "PENDING,    IN_TRANSIT, true",
      "PENDING,    CANCELLED,  true",
      "PENDING,    COMPLETED,  false",
      "PENDING,    DELAYED,    false",
      "IN_TRANSIT, DELAYED,    true",
      "IN_TRANSIT, COMPLETED,  true",
      "IN_TRANSIT, CANCELLED,  false",
      "IN_TRANSIT, PENDING,    false",
      "DELAYED,    COMPLETED,  true",
      "DELAYED,    IN_TRANSIT, false",
      "DELAYED,    CANCELLED,  false",
      "COMPLETED,  IN_TRANSIT, false",
      "CANCELLED,  PENDING,    false"
  })
  void transitionMatrix(TripStatus from, TripStatus to, boolean allowed) {
    assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
  }

  // @req TRIP-007
  @ParameterizedTest
  @EnumSource(TripStatus.class)
  void noStatusTransitionsToItself(TripStatus status) {
    assertThat(status.canTransitionTo(status)).isFalse();
  }

  // @req TRIP-007
  @ParameterizedTest
  @EnumSource(TripStatus.class)
  void onlyCompletedAndCancelledAreFinal(TripStatus status) {
    assertThat(status.isFinal()).isEqualTo(Set.of(TripStatus.COMPLETED, TripStatus.CANCELLED).contains(status));
  }
}
