package com.logistics.logistics.infrastructure.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.logistics.logistics.application.service.TripService;
import com.logistics.logistics.infrastructure.controller.api.TripsApi;
import com.logistics.logistics.infrastructure.controller.dto.TripDTO;
import com.logistics.logistics.infrastructure.controller.dto.TripPageDTO;
import com.logistics.logistics.infrastructure.controller.dto.TripRequestDTO;
import com.logistics.logistics.infrastructure.controller.dto.TripStatusDTO;
import com.logistics.logistics.infrastructure.controller.dto.TripStatusUpdateDTO;
import com.logistics.logistics.infrastructure.controller.mapper.ApiMapper;

/** Rotas de viagens. Caminhos, validação e documentação vêm de {@link TripsApi}, gerada do contrato. */
@RestController
public class TripController implements TripsApi {

  private final TripService tripService;

  public TripController(TripService tripService) {
    this.tripService = tripService;
  }

  @Override
  public ResponseEntity<TripDTO> createTrip(TripRequestDTO request) {
    TripDTO created = ApiMapper.toDto(tripService.createTrip(ApiMapper.toCommand(request)));
    URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.getId()).toUri();
    return ResponseEntity.created(location).body(created);
  }

  @Override
  public ResponseEntity<TripPageDTO> listTrips(TripStatusDTO status, Integer page, Integer size) {
    return ResponseEntity.ok(ApiMapper.toTripPage(tripService.listTrips(ApiMapper.toDomain(status), page, size)));
  }

  @Override
  public ResponseEntity<TripDTO> getTrip(UUID id) {
    return ResponseEntity.ok(ApiMapper.toDto(tripService.getTrip(id)));
  }

  @Override
  public ResponseEntity<TripDTO> updateTripStatus(UUID id, TripStatusUpdateDTO update) {
    return ResponseEntity.ok(ApiMapper.toDto(tripService.updateStatus(id, ApiMapper.toDomain(update.getStatus()))));
  }
}
