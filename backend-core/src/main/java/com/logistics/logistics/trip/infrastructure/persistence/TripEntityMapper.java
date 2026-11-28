package com.logistics.logistics.trip.infrastructure.persistence;

import com.logistics.logistics.shared.domain.Coordinates;
import com.logistics.logistics.trip.domain.Trip;

final class TripEntityMapper {

  private TripEntityMapper() {}

  static TripEntity toEntity(Trip trip) {
    TripEntity entity = new TripEntity();
    entity.setId(trip.id());
    entity.setOriginLat(trip.origin().lat());
    entity.setOriginLng(trip.origin().lng());
    entity.setDestLat(trip.destination().lat());
    entity.setDestLng(trip.destination().lng());
    entity.setOriginLabel(trip.originLabel());
    entity.setDestinationLabel(trip.destinationLabel());
    entity.setPayloadWeight(trip.payloadWeight());
    entity.setStatus(trip.status());
    entity.setDepartureTime(trip.departureTime());
    entity.setPredictedArrival(trip.predictedArrival());
    entity.setActualArrival(trip.actualArrival());
    entity.setEtaModelVersion(trip.etaModelVersion());
    entity.setEtaSource(trip.etaSource());
    entity.setCreatedAt(trip.createdAt());
    return entity;
  }

  static Trip toDomain(TripEntity entity) {
    return new Trip(
        entity.getId(),
        new Coordinates(entity.getOriginLat(), entity.getOriginLng()),
        new Coordinates(entity.getDestLat(), entity.getDestLng()),
        entity.getOriginLabel(),
        entity.getDestinationLabel(),
        entity.getPayloadWeight(),
        entity.getDepartureTime(),
        entity.getStatus(),
        entity.getPredictedArrival(),
        entity.getEtaModelVersion(),
        entity.getEtaSource(),
        entity.getActualArrival(),
        entity.getCreatedAt());
  }
}
