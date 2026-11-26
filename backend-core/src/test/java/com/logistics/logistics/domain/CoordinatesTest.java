package com.logistics.logistics.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CoordinatesTest {

  @ParameterizedTest(name = "({0}, {1}) é inválida")
  @CsvSource({ "-90.1, 0", "90.1, 0", "0, -180.1", "0, 180.1" })
  void rejectsOutOfRangeValues(double lat, double lng) {
    assertThatThrownBy(() -> new Coordinates(lat, lng)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsMissingValues() {
    assertThatThrownBy(() -> new Coordinates(null, 0.0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Coordinates(0.0, null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void acceptsBoundaries() {
    new Coordinates(-90.0, -180.0);
    new Coordinates(90.0, 180.0);
  }

  @Test
  void distanceIsSymmetricAndZeroToItself() {
    Coordinates sp = new Coordinates(-23.5505, -46.6333);
    Coordinates bh = new Coordinates(-19.9167, -43.9345);
    assertThat(sp.distanceKmTo(sp)).isZero();
    assertThat(sp.distanceKmTo(bh)).isCloseTo(bh.distanceKmTo(sp), within(1e-9));
    assertThat(sp.distanceKmTo(bh)).isCloseTo(490, within(5.0));
  }

  @Test
  void antipodalPointsAreHalfTheEarthCircumferenceApart() {
    double distance = new Coordinates(0.0, 0.0).distanceKmTo(new Coordinates(0.0, 180.0));
    assertThat(distance).isCloseTo(Math.PI * 6371.0088, within(0.01));
  }
}
