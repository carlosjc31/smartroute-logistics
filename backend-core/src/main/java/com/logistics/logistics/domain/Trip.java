package com.logistics.logistics.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record Trip(
  UUID id,
  Coordinates origin,
  Coordinates destination,
  Double payloadWeight,
  LocalDateTime departureTime,
  TripStatus status
) {
  public Trip {
    if (payloadWeight < 0)
      throw new IllegalArgumentException("Peso não pode ser negativo");
    if (origin.equals(destination))
      throw new IllegalArgumentException("Origem e destino não podem ser iguais");
  }
  public enum TripStatus {
    PENDING,
    IN_TRANSIT,
    COMPLETED,
    DELEYED
  }
}
