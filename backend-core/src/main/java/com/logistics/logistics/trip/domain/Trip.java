package com.logistics.logistics.trip.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.logistics.logistics.shared.domain.Coordinates;

/**
 * Viagem de entrega. Imutável: operações de negócio devolvem uma nova instância.
 */
public record Trip(
  UUID id,
  Coordinates origin,
  Coordinates destination,
  String originLabel,
  String destinationLabel,
  Double payloadWeight,
  OffsetDateTime departureTime,
  TripStatus status,
  OffsetDateTime predictedArrival,
  String etaModelVersion,
  EtaSource etaSource,
  OffsetDateTime actualArrival,
  OffsetDateTime createdAt
) {
  public Trip {
    require(id, "Id é obrigatório");
    require(origin, "Origem é obrigatória");
    require(destination, "Destino é obrigatório");
    require(payloadWeight, "Peso é obrigatório");
    require(departureTime, "Horário de saída é obrigatório");
    require(status, "Status é obrigatório");
    require(createdAt, "Data de criação é obrigatória");
    if (payloadWeight < 0)
      throw new IllegalArgumentException("Peso não pode ser negativo");
    if (origin.equals(destination))
      throw new IllegalArgumentException("Origem e destino não podem ser iguais");
  }

  /** Cria uma nova viagem pendente, ainda sem previsão de chegada. */
  public static Trip create(Coordinates origin, Coordinates destination, String originLabel,
      String destinationLabel, Double payloadWeight, OffsetDateTime departureTime, OffsetDateTime now) {
    return new Trip(UUID.randomUUID(), origin, destination, originLabel, destinationLabel, payloadWeight,
        departureTime, TripStatus.PENDING, null, null, null, null, now);
  }

  public double distanceKm() {
    return origin.distanceKmTo(destination);
  }

  /** Soma a duração prevista (em horas decimais) ao horário de saída, sem perder os minutos. */
  public OffsetDateTime estimateArrival(double predictedHours) {
    if (predictedHours < 0 || Double.isNaN(predictedHours))
      throw new IllegalArgumentException("Duração prevista inválida: " + predictedHours);
    return departureTime.plusMinutes(Math.round(predictedHours * 60));
  }

  public Trip withPrediction(EtaPrediction prediction) {
    return new Trip(id, origin, destination, originLabel, destinationLabel, payloadWeight, departureTime, status,
        estimateArrival(prediction.hours()), prediction.modelVersion(), prediction.source(), actualArrival, createdAt);
  }

  /** Avança o ciclo de vida. Ao concluir, registra a chegada real (base para re-treino do modelo). */
  public Trip transitionTo(TripStatus target, OffsetDateTime now) {
    if (!status.canTransitionTo(target))
      throw new InvalidStatusTransitionException(status, target);
    OffsetDateTime arrival = target == TripStatus.COMPLETED ? now : actualArrival;
    return new Trip(id, origin, destination, originLabel, destinationLabel, payloadWeight, departureTime, target,
        predictedArrival, etaModelVersion, etaSource, arrival, createdAt);
  }

  private static void require(Object value, String message) {
    if (value == null)
      throw new IllegalArgumentException(message);
  }
}
