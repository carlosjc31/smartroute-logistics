package com.logistics.logistics.trip.application;

import java.time.OffsetDateTime;

import com.logistics.logistics.shared.domain.Coordinates;

public record CreateTripCommand(
  Coordinates origin,
  Coordinates destination,
  String originLabel,
  String destinationLabel,
  Double payloadWeight,
  OffsetDateTime departureTime
) {}
