package com.logistics.logistics.trip.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.logistics.logistics.shared.application.PageResult;
import com.logistics.logistics.trip.application.TripRepositoryPort;
import com.logistics.logistics.trip.domain.Trip;
import com.logistics.logistics.trip.domain.TripStatus;

@Component
public class TripPersistenceAdapter implements TripRepositoryPort {

  private final TripJpaRepository repository;

  public TripPersistenceAdapter(TripJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public Trip save(Trip trip) {
    return TripEntityMapper.toDomain(repository.save(TripEntityMapper.toEntity(trip)));
  }

  @Override
  public Optional<Trip> findById(UUID id) {
    return repository.findById(id).map(TripEntityMapper::toDomain);
  }

  @Override
  public PageResult<Trip> findAll(TripStatus status, int page, int size) {
    PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<TripEntity> result = status == null
        ? repository.findAll(pageable)
        : repository.findByStatus(status, pageable);
    return new PageResult<>(result.map(TripEntityMapper::toDomain).getContent(),
        result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
  }
}
