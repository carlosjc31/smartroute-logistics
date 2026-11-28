package com.logistics.logistics.trip.application;

import java.util.Optional;
import java.util.UUID;

import com.logistics.logistics.shared.application.PageResult;
import com.logistics.logistics.trip.domain.Trip;
import com.logistics.logistics.trip.domain.TripStatus;

public interface TripRepositoryPort {
  Trip save(Trip trip);

  Optional<Trip> findById(UUID id);

  /** Lista viagens da mais recente para a mais antiga, opcionalmente filtrando por status. */
  PageResult<Trip> findAll(TripStatus status, int page, int size);
}
