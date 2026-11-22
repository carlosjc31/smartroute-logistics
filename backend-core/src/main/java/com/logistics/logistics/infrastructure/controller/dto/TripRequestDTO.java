package com.logistics.logistics.infrastructure.controller.dto;

import java.time.OffsetDateTime;

import com.logistics.logistics.application.service.CreateTripCommand;
import com.logistics.logistics.domain.Coordinates;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TripRequestDTO(
  @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double originLat,
  @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double originLng,
  @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double destLat,
  @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double destLng,
  @Schema(example = "São Paulo, SP") @Size(max = 200) String originLabel,
  @Schema(example = "Rio de Janeiro, RJ") @Size(max = 200) String destinationLabel,
  @Schema(description = "Peso da carga em toneladas") @NotNull @Positive Double payloadWeight,
  @Schema(description = "Horário de saída com fuso (ISO-8601)", example = "2030-01-01T08:00:00-03:00")
  @NotNull @Future OffsetDateTime departureTime
) {
  public CreateTripCommand toCommand() {
    return new CreateTripCommand(
        new Coordinates(originLat, originLng),
        new Coordinates(destLat, destLng),
        originLabel,
        destinationLabel,
        payloadWeight,
        departureTime);
  }
}
