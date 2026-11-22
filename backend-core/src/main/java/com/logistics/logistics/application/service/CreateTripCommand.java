package com.logistics.logistics.application.service;

import java.time.OffsetDateTime;

import com.logistics.logistics.domain.Coordinates;

public record CreateTripCommand(
  Coordinates origin,
  Coordinates destination,
  String originLabel,
  String destinationLabel,
  Double payloadWeight,
  OffsetDateTime departureTime
) {}
