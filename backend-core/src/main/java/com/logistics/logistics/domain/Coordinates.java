package com.logistics.logistics.domain;

import java.util.Objects;

public record Coordinates(Double lat, Double lng) {

  private static final double EARTH_RADIUS_KM = 6371.0088;

  public Coordinates {
    Objects.requireNonNull(lat, "Latitude é obrigatória");
    Objects.requireNonNull(lng, "Longitude é obrigatória");
    if (lat < -90 || lat > 90)
      throw new IllegalArgumentException("Latitude deve estar entre -90 e 90");
    if (lng < -180 || lng > 180)
      throw new IllegalArgumentException("Longitude deve estar entre -180 e 180");
  }

  /** Distância em linha reta (fórmula de Haversine), em quilômetros. */
  public double distanceKmTo(Coordinates other) {
    double dLat = Math.toRadians(other.lat - lat);
    double dLng = Math.toRadians(other.lng - lng);
    double a = Math.pow(Math.sin(dLat / 2), 2)
        + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(other.lat))
        * Math.pow(Math.sin(dLng / 2), 2);
    return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(a)));
  }
}
