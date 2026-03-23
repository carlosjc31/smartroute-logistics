package com.logistics.logistics.application.services;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.logistics.logistics.domain.Trip;

import com.logistics.logistics.infrastructure.adapters.AIClientAdapter;
import com.logistics.logistics.infrastructure.persistence.TripEntity.TripStatus;
import com.logistics.logistics.infrastructure.persistence.TripEntity;
import com.logistics.logistics.infrastructure.persistence.TripRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TripService {

  private final TripRepository repository;
  private final AIClientAdapter aiClient;

  @Transactional
  public TripEntity createNewTrip(Trip trip) {
        // 1. Chamar o Alchemist (IA) para prever o ETA
    var prediction = aiClient.getETAPrediction(trip);

        // 2. Mapear Domínio -> Entidade de Banco
    TripEntity entity = new TripEntity();
    entity.setOriginLat(trip.origin().lat());
    entity.setOriginLng(trip.origin().lng());
    entity.setDestLat(trip.destination().lat());
    entity.setDestLng(trip.destination().lng());
    entity.setPayloadWeight(trip.payloadWwight());
    //entity.setStatus(TripEntity.TripStatus.PENDING);
    entity.setStatus(TripStatus.PENDING);
    entity.setPredictedArrival(LocalDateTime.now().plusHours((prediction.predicted_arrival()).longValue()));

        // 3. Persistir
    return repository.save(entity);
  }
}
