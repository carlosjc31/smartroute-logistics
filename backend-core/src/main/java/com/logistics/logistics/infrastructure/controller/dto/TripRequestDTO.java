package com.logistics.logistics.infrastructure.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;


import com.logistics.logistics.domain.Coordinates;
import com.logistics.logistics.domain.Trip;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;


public record TripRequestDTO(
  Double originLat,
  Double originLng,
  Double destLat,
  Double destLng,
  @Positive Double payloadWeight,
  @Future LocalDateTime departureTime
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
