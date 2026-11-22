package com.logistics.logistics.domain;

import java.time.LocalDateTime;
import java.util.Objects;
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
    Objects.requireNonNull(origin, "Origem é obrigatória");
    Objects.requireNonNull(destination, "Destino é obrigatório");
    Objects.requireNonNull(payloadWeight, "Peso é obrigatório");
    Objects.requireNonNull(departureTime, "Horário de saída é obrigatório");
    if (payloadWeight < 0)
      throw new IllegalArgumentException("Peso não pode ser negativo");
    if (origin.equals(destination))
      throw new IllegalArgumentException("Origem e destino não podem ser iguais");
  }

  public double distanceKm() {
    return origin.distanceKmTo(destination);
  }

  /** Soma a duração prevista (em horas decimais) ao horário de saída, sem perder os minutos. */
  public LocalDateTime estimateArrival(double predictedHours) {
    if (predictedHours < 0 || Double.isNaN(predictedHours))
      throw new IllegalArgumentException("Duração prevista inválida: " + predictedHours);
    return departureTime.plusMinutes(Math.round(predictedHours * 60));
  }

  public enum TripStatus {
    PENDING,
    IN_TRANSIT,
    COMPLETED,
    DELAYED
  }
}
