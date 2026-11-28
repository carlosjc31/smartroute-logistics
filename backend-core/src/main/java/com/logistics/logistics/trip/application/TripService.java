package com.logistics.logistics.trip.application;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.logistics.audit.AuditAction;
import com.logistics.logistics.audit.AuditService;
import com.logistics.logistics.shared.application.PageResult;
import com.logistics.logistics.trip.TripCreated;
import com.logistics.logistics.trip.TripStatusChanged;
import com.logistics.logistics.trip.domain.EtaPrediction;
import com.logistics.logistics.trip.domain.HeuristicEtaEstimator;
import com.logistics.logistics.trip.domain.Trip;
import com.logistics.logistics.trip.domain.TripStatus;

@Service
public class TripService {

  private static final Logger log = LoggerFactory.getLogger(TripService.class);

  private final TripRepositoryPort repository;
  private final EtaPredictionPort etaPrediction;
  private final AuditService audit;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public TripService(TripRepositoryPort repository, EtaPredictionPort etaPrediction, AuditService audit,
      ApplicationEventPublisher events, Clock clock) {
    this.repository = repository;
    this.etaPrediction = etaPrediction;
    this.audit = audit;
    this.events = events;
    this.clock = clock;
  }

  @Transactional
  public Trip createTrip(CreateTripCommand command) {
    Trip trip = Trip.create(command.origin(), command.destination(), command.originLabel(),
        command.destinationLabel(), command.payloadWeight(), command.departureTime(), OffsetDateTime.now(clock));
    Trip saved = repository.save(trip.withPrediction(predict(trip)));
    audit.record(AuditAction.TRIP_CREATED, AuditService.TRIP, saved.id(), null, TripAuditSnapshot.of(saved));
    events.publishEvent(new TripCreated(saved.id(), saved.status().name(), saved.departureTime(), saved.createdAt()));
    return saved;
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
    Trip current = getTrip(id);
    Trip saved = repository.save(current.transitionTo(target, OffsetDateTime.now(clock)));
    audit.record(AuditAction.TRIP_STATUS_CHANGED, AuditService.TRIP, saved.id(), TripAuditSnapshot.of(current),
        TripAuditSnapshot.of(saved));
    events.publishEvent(new TripStatusChanged(saved.id(), current.status().name(), saved.status().name(),
        OffsetDateTime.now(clock)));
    return saved;
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
