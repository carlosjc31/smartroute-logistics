package com.logistics.logistics.trip.infrastructure.web;

import java.util.LinkedHashSet;

import com.logistics.logistics.api.dto.EtaDTO;
import com.logistics.logistics.api.dto.EtaSourceDTO;
import com.logistics.logistics.api.dto.LocationDTO;
import com.logistics.logistics.api.dto.TripDTO;
import com.logistics.logistics.api.dto.TripPageDTO;
import com.logistics.logistics.api.dto.TripRequestDTO;
import com.logistics.logistics.api.dto.TripStatusDTO;
import com.logistics.logistics.shared.application.PageResult;
import com.logistics.logistics.shared.domain.Coordinates;
import com.logistics.logistics.trip.application.CreateTripCommand;
import com.logistics.logistics.trip.domain.Trip;
import com.logistics.logistics.trip.domain.TripStatus;

/**
 * Converte entre os DTOs gerados do contrato (contracts/api/openapi.yaml) e o domínio de viagens. Os enums
 * são convertidos pelo nome; {@code TripApiMapperTest} confere que os dois lados têm os mesmos valores.
 */
final class TripApiMapper {

  private TripApiMapper() {}

  static CreateTripCommand toCommand(TripRequestDTO dto) {
    return new CreateTripCommand(
        new Coordinates(dto.getOriginLat(), dto.getOriginLng()),
        new Coordinates(dto.getDestLat(), dto.getDestLng()),
        dto.getOriginLabel(),
        dto.getDestinationLabel(),
        dto.getPayloadWeight(),
        dto.getDepartureTime());
  }

  static TripStatus toDomain(TripStatusDTO status) {
    return status == null ? null : TripStatus.valueOf(status.name());
  }

  static TripStatusDTO toDto(TripStatus status) {
    return TripStatusDTO.valueOf(status.name());
  }

  static TripDTO toDto(Trip trip) {
    LinkedHashSet<TripStatusDTO> transitions = new LinkedHashSet<>();
    trip.status().allowedTransitions().stream().sorted().map(TripApiMapper::toDto).forEach(transitions::add);
    return new TripDTO(
        trip.id(),
        location(trip.originLabel(), trip.origin()),
        location(trip.destinationLabel(), trip.destination()),
        trip.payloadWeight(),
        Math.round(trip.distanceKm() * 10) / 10.0,
        toDto(trip.status()),
        transitions,
        trip.departureTime(),
        trip.predictedArrival(),
        trip.etaSource() == null ? null
            : new EtaDTO(EtaSourceDTO.valueOf(trip.etaSource().name()), trip.etaModelVersion()),
        trip.actualArrival(),
        trip.createdAt());
  }

  static TripPageDTO toPage(PageResult<Trip> page) {
    return new TripPageDTO(page.map(TripApiMapper::toDto).items(), page.page(), page.size(), page.totalItems(),
        page.totalPages());
  }

  private static LocationDTO location(String label, Coordinates coordinates) {
    return new LocationDTO(label, coordinates.lat(), coordinates.lng());
  }
}
