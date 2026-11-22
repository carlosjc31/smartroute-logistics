package com.logistics.logistics.infrastructure.controller.dto;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import com.logistics.logistics.domain.Coordinates;
import com.logistics.logistics.domain.EtaSource;
import com.logistics.logistics.domain.Trip;
import com.logistics.logistics.domain.TripStatus;

public record TripResponseDTO(
  UUID id,
  Location origin,
  Location destination,
  Double payloadWeight,
  double distanceKm,
  TripStatus status,
  Set<TripStatus> allowedTransitions,
  OffsetDateTime departureTime,
  OffsetDateTime predictedArrival,
  Eta eta,
  OffsetDateTime actualArrival,
  OffsetDateTime createdAt
) {
  public record Location(String label, double lat, double lng) {
    static Location of(String label, Coordinates coordinates) {
      return new Location(label, coordinates.lat(), coordinates.lng());
    }
  }

  public record Eta(EtaSource source, String modelVersion) {}

  public static TripResponseDTO from(Trip trip) {
    return new TripResponseDTO(
        trip.id(),
        Location.of(trip.originLabel(), trip.origin()),
        Location.of(trip.destinationLabel(), trip.destination()),
        trip.payloadWeight(),
        Math.round(trip.distanceKm() * 10) / 10.0,
        trip.status(),
        trip.status().allowedTransitions(),
        trip.departureTime(),
        trip.predictedArrival(),
        trip.etaSource() == null ? null : new Eta(trip.etaSource(), trip.etaModelVersion()),
        trip.actualArrival(),
        trip.createdAt());
  }
}
