package com.logistics.logistics.infrastructure.controller.mapper;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import com.logistics.logistics.application.PageResult;
import com.logistics.logistics.application.audit.AuditAction;
import com.logistics.logistics.application.audit.AuditEntry;
import com.logistics.logistics.application.service.CreateTripCommand;
import com.logistics.logistics.domain.Coordinates;
import com.logistics.logistics.domain.GeoLocation;
import com.logistics.logistics.domain.Trip;
import com.logistics.logistics.domain.TripStatus;
import com.logistics.logistics.infrastructure.controller.dto.AuditActionDTO;
import com.logistics.logistics.infrastructure.controller.dto.AuditEntryDTO;
import com.logistics.logistics.infrastructure.controller.dto.AuditEntryPageDTO;
import com.logistics.logistics.infrastructure.controller.dto.EtaDTO;
import com.logistics.logistics.infrastructure.controller.dto.EtaSourceDTO;
import com.logistics.logistics.infrastructure.controller.dto.GeocodeResultDTO;
import com.logistics.logistics.infrastructure.controller.dto.LocationDTO;
import com.logistics.logistics.infrastructure.controller.dto.TripDTO;
import com.logistics.logistics.infrastructure.controller.dto.TripPageDTO;
import com.logistics.logistics.infrastructure.controller.dto.TripRequestDTO;
import com.logistics.logistics.infrastructure.controller.dto.TripStatusDTO;

/**
 * Converte entre os DTOs gerados do contrato (contracts/api/openapi.yaml) e os tipos do domínio e da
 * aplicação. Os enums são convertidos pelo nome: um valor que existe só de um lado falha no
 * {@code ApiMapperTest}, que cobre todos os valores.
 */
public final class ApiMapper {

  private ApiMapper() {}

  public static CreateTripCommand toCommand(TripRequestDTO dto) {
    return new CreateTripCommand(
        new Coordinates(dto.getOriginLat(), dto.getOriginLng()),
        new Coordinates(dto.getDestLat(), dto.getDestLng()),
        dto.getOriginLabel(),
        dto.getDestinationLabel(),
        dto.getPayloadWeight(),
        dto.getDepartureTime());
  }

  public static TripStatus toDomain(TripStatusDTO status) {
    return status == null ? null : TripStatus.valueOf(status.name());
  }

  public static TripStatusDTO toDto(TripStatus status) {
    return TripStatusDTO.valueOf(status.name());
  }

  public static AuditActionDTO toDto(AuditAction action) {
    return AuditActionDTO.valueOf(action.name());
  }

  public static TripDTO toDto(Trip trip) {
    LinkedHashSet<TripStatusDTO> transitions = new LinkedHashSet<>();
    trip.status().allowedTransitions().stream().sorted().map(ApiMapper::toDto).forEach(transitions::add);
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

  public static TripPageDTO toTripPage(PageResult<Trip> page) {
    return new TripPageDTO(page.map(ApiMapper::toDto).items(), page.page(), page.size(), page.totalItems(),
        page.totalPages());
  }

  public static GeocodeResultDTO toDto(GeoLocation location) {
    return new GeocodeResultDTO(location.displayName(), location.coordinates().lat(), location.coordinates().lng());
  }

  public static AuditEntryDTO toDto(AuditEntry entry) {
    return new AuditEntryDTO(entry.id(), entry.occurredAt(), entry.actor(), toDto(entry.action()),
        copy(entry.before()), copy(entry.after()));
  }

  public static AuditEntryPageDTO toAuditPage(PageResult<AuditEntry> page) {
    return new AuditEntryPageDTO(page.map(ApiMapper::toDto).items(), page.page(), page.size(), page.totalItems(),
        page.totalPages());
  }

  private static LocationDTO location(String label, Coordinates coordinates) {
    return new LocationDTO(label, coordinates.lat(), coordinates.lng());
  }

  private static Map<String, Object> copy(Map<String, Object> state) {
    return state == null ? null : new LinkedHashMap<>(state);
  }
}
