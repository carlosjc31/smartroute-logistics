package com.logistics.logistics.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.logistics.logistics.application.PageResult;
import com.logistics.logistics.domain.Trip;
import com.logistics.logistics.domain.TripStatus;

public interface TripRepositoryPort {
  Trip save(Trip trip);

  Optional<Trip> findById(UUID id);

  /** Lista viagens da mais recente para a mais antiga, opcionalmente filtrando por status. */
  PageResult<Trip> findAll(TripStatus status, int page, int size);
}
