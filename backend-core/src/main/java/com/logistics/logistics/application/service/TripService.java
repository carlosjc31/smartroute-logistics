package com.logistics.logistics.application.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.logistics.application.PageResult;
import com.logistics.logistics.application.exception.EtaPredictionUnavailableException;
import com.logistics.logistics.application.exception.TripNotFoundException;
import com.logistics.logistics.application.port.out.EtaPredictionPort;
import com.logistics.logistics.application.port.out.TripRepositoryPort;
import com.logistics.logistics.domain.EtaPrediction;
import com.logistics.logistics.domain.HeuristicEtaEstimator;
import com.logistics.logistics.domain.Trip;
import com.logistics.logistics.domain.TripStatus;

@Service
public class TripService {

  private static final Logger log = LoggerFactory.getLogger(TripService.class);

  private final TripRepositoryPort repository;
  private final EtaPredictionPort etaPrediction;
  private final Clock clock;

  public TripService(TripRepositoryPort repository, EtaPredictionPort etaPrediction, Clock clock) {
    this.repository = repository;
    this.etaPrediction = etaPrediction;
    this.clock = clock;
  }

  @Transactional
  public Trip createTrip(CreateTripCommand command) {
    Trip trip = Trip.create(command.origin(), command.destination(), command.originLabel(),
        command.destinationLabel(), command.payloadWeight(), command.departureTime(), OffsetDateTime.now(clock));
    return repository.save(trip.withPrediction(predict(trip)));
  }

  @Transactional(readOnly = true)
  public Trip getTrip(UUID id) {
    return repository.findById(id).orElseThrow(() -> new TripNotFoundException(id));
  }

  @Transactional(readOnly = true)
  public PageResult<Trip> listTrips(TripStatus status, int page, int size) {
    return repository.findAll(status, page, size);
  }

  @Transactional
  public Trip updateStatus(UUID id, TripStatus target) {
    Trip updated = getTrip(id).transitionTo(target, OffsetDateTime.now(clock));
    return repository.save(updated);
  }

  private EtaPrediction predict(Trip trip) {
    try {
      return etaPrediction.predict(trip);
    } catch (EtaPredictionUnavailableException e) {
      log.warn("ai-engine indisponível, usando estimativa heurística: {}", e.getMessage());
      return HeuristicEtaEstimator.estimate(trip);
    }
  }
}
