package com.logistics.logistics.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EtaEstimationTest {

  private static final ZoneOffset BRT = ZoneOffset.ofHours(-3);

  private static Trip trip(double weight, int departureHour) {
    return Trip.create(new Coordinates(-23.55, -46.63), new Coordinates(-22.90, -43.17), null, null, weight,
        OffsetDateTime.of(2030, 1, 1, departureHour, 0, 0, 0, BRT), OffsetDateTime.now(ZoneOffset.UTC));
  }

  @ParameterizedTest(name = "saída às {0}h: pico = {1}")
  @CsvSource({ "6,false", "7,true", "9,true", "10,false", "16,false", "17,true", "19,true", "20,false" })
  void heuristicUsesLocalDepartureHourForPeak(int hour, boolean peak) {
    Trip trip = trip(0.0, hour);
    double base = trip.distanceKm() / 80.0;

    EtaPrediction eta = HeuristicEtaEstimator.estimate(trip);

    assertThat(eta.hours()).isCloseTo(peak ? base * 1.5 : base, within(1e-9));
    assertThat(eta.source()).isEqualTo(EtaSource.FALLBACK);
    assertThat(eta.modelVersion()).isEqualTo(HeuristicEtaEstimator.VERSION);
    assertThat(eta.confidence()).isNull();
  }

  @Test
  void heuristicAddsDelayProportionalToWeight() {
    Trip empty = trip(0.0, 12);
    Trip loaded = trip(20.0, 12);
    double ratio = HeuristicEtaEstimator.estimate(loaded).hours() / HeuristicEtaEstimator.estimate(empty).hours();
    assertThat(ratio).isCloseTo(1.10, within(1e-9));
  }

  @Test
  void arrivalRoundsToNearestMinuteAndKeepsOffset() {
    Trip trip = trip(10.0, 8);
    // 1,99 h = 119,4 min -> 119 min; 2,01 h = 120,6 min -> 121 min
    assertThat(trip.estimateArrival(1.99)).isEqualTo(trip.departureTime().plusMinutes(119));
    assertThat(trip.estimateArrival(2.01)).isEqualTo(trip.departureTime().plusMinutes(121));
    assertThat(trip.estimateArrival(0)).isEqualTo(trip.departureTime());
    assertThat(trip.estimateArrival(2.0).getOffset()).isEqualTo(BRT);
  }

  @Test
  void rejectsInvalidDurations() {
    Trip trip = trip(10.0, 8);
    assertThatThrownBy(() -> trip.estimateArrival(-0.1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> trip.estimateArrival(Double.NaN)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new EtaPrediction(-1, null, "v", EtaSource.MODEL))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new EtaPrediction(1, null, "v", null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNegativeWeightAndMissingFields() {
    assertThatThrownBy(() -> trip(-1.0, 8)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Trip.create(null, new Coordinates(0.0, 0.0), null, null, 1.0,
        OffsetDateTime.now(), OffsetDateTime.now())).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Trip.create(new Coordinates(1.0, 1.0), new Coordinates(0.0, 0.0), null, null, 1.0,
        null, OffsetDateTime.now())).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void transitionsKeepPredictionAndOnlyCompletionSetsArrival() {
    OffsetDateTime later = OffsetDateTime.of(2030, 1, 1, 12, 0, 0, 0, BRT);
    Trip trip = trip(10.0, 8).withPrediction(new EtaPrediction(4.5, 0.9, "v1", EtaSource.MODEL));

    Trip delayed = trip.transitionTo(TripStatus.IN_TRANSIT, later).transitionTo(TripStatus.DELAYED, later);
    assertThat(delayed.actualArrival()).isNull();
    assertThat(delayed.predictedArrival()).isEqualTo(trip.predictedArrival());
    assertThat(delayed.etaSource()).isEqualTo(EtaSource.MODEL);

    Trip completed = delayed.transitionTo(TripStatus.COMPLETED, later.plusHours(1));
    assertThat(completed.actualArrival()).isEqualTo(later.plusHours(1));
    assertThat(completed.id()).isEqualTo(trip.id());
  }
}
