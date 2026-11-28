package com.logistics.logistics.trip.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.logistics.logistics.shared.domain.Coordinates;

class TripTest {

  static final Coordinates SAO_PAULO = new Coordinates(-23.5505, -46.6333);
  static final Coordinates RIO = new Coordinates(-22.9068, -43.1729);
  static final OffsetDateTime DEPARTURE = OffsetDateTime.of(2030, 1, 1, 8, 0, 0, 0, ZoneOffset.ofHours(-3));
  static final OffsetDateTime NOW = OffsetDateTime.of(2029, 12, 31, 12, 0, 0, 0, ZoneOffset.UTC);

  static Trip trip(Coordinates origin, Coordinates destination, Double weight) {
    return Trip.create(origin, destination, "SP", "RJ", weight, DEPARTURE, NOW);
  }

  // @req TRIP-001
  @Test
  void newTripIsPendingWithoutPrediction() {
    Trip trip = trip(SAO_PAULO, RIO, 10.0);
    assertThat(trip.status()).isEqualTo(TripStatus.PENDING);
    assertThat(trip.predictedArrival()).isNull();
    assertThat(trip.createdAt()).isEqualTo(NOW);
  }

  // @req TRIP-003
  @Test
  void distanceBetweenSaoPauloAndRioIsAbout360Km() {
    assertThat(trip(SAO_PAULO, RIO, 10.0).distanceKm()).isCloseTo(361, within(5.0));
  }

  // @req TRIP-003
  @Test
  void distanceIsFiniteForVeryClosePoints() {
    Coordinates nearby = new Coordinates(-23.5505, -46.63330001);
    assertThat(SAO_PAULO.distanceKmTo(nearby)).isFinite().isGreaterThanOrEqualTo(0);
  }

  // @req TRIP-003
  @Test
  void predictionKeepsMinutesAndSource() {
    Trip trip = trip(SAO_PAULO, RIO, 10.0)
        .withPrediction(new EtaPrediction(2.9, 0.8, "v1", EtaSource.MODEL));
    assertThat(trip.predictedArrival()).isEqualTo(DEPARTURE.plusHours(2).plusMinutes(54));
    assertThat(trip.etaSource()).isEqualTo(EtaSource.MODEL);
    assertThat(trip.etaModelVersion()).isEqualTo("v1");
  }

  // @req TRIP-008
  @Test
  void completingRecordsActualArrival() {
    OffsetDateTime arrival = DEPARTURE.plusHours(5);
    Trip done = trip(SAO_PAULO, RIO, 10.0)
        .transitionTo(TripStatus.IN_TRANSIT, DEPARTURE)
        .transitionTo(TripStatus.COMPLETED, arrival);
    assertThat(done.status()).isEqualTo(TripStatus.COMPLETED);
    assertThat(done.actualArrival()).isEqualTo(arrival);
  }

  // @req TRIP-007
  @Test
  void rejectsInvalidTransition() {
    assertThatThrownBy(() -> trip(SAO_PAULO, RIO, 10.0).transitionTo(TripStatus.COMPLETED, NOW))
        .isInstanceOf(InvalidStatusTransitionException.class);
  }

  // @req TRIP-007
  @Test
  void finalStatusesHaveNoTransitions() {
    assertThat(TripStatus.COMPLETED.isFinal()).isTrue();
    assertThat(TripStatus.CANCELLED.isFinal()).isTrue();
    assertThat(TripStatus.PENDING.allowedTransitions()).containsExactlyInAnyOrder(TripStatus.IN_TRANSIT, TripStatus.CANCELLED);
  }

  // @req TRIP-002
  @Test
  void rejectsNullWeight() {
    assertThatThrownBy(() -> trip(SAO_PAULO, RIO, null)).isInstanceOf(IllegalArgumentException.class);
  }

  // @req TRIP-002
  @Test
  void rejectsSameOriginAndDestination() {
    assertThatThrownBy(() -> trip(SAO_PAULO, SAO_PAULO, 10.0)).isInstanceOf(IllegalArgumentException.class);
  }

  // @req TRIP-002
  @Test
  void rejectsInvalidLatitude() {
    assertThatThrownBy(() -> new Coordinates(91.0, 0.0)).isInstanceOf(IllegalArgumentException.class);
  }

  // @req TRIP-004
  @Test
  void heuristicAppliesPeakMultiplier() {
    Trip peak = trip(SAO_PAULO, RIO, 0.0); // 08:00, horário de pico
    EtaPrediction eta = HeuristicEtaEstimator.estimate(peak);
    assertThat(eta.source()).isEqualTo(EtaSource.FALLBACK);
    assertThat(eta.hours()).isCloseTo(peak.distanceKm() / 80 * 1.5, within(0.001));
  }
}
