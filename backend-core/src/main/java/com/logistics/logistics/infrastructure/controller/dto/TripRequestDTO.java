package com.logistics.logistics.infrastructure.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.logistics.logistics.domain.Coordinates;
import com.logistics.logistics.domain.Trip;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TripRequestDTO(
  @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double originLat,
  @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double originLng,
  @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double destLat,
  @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double destLng,
  @NotNull @Positive Double payloadWeight,
  @NotNull @Future LocalDateTime departureTime
) {
  public Trip toDomain() {
    return new Trip(
        UUID.randomUUID(),
        new Coordinates(originLat, originLng),
        new Coordinates(destLat, destLng),
        payloadWeight,
        departureTime,
        Trip.TripStatus.PENDING
    );
  }
}
