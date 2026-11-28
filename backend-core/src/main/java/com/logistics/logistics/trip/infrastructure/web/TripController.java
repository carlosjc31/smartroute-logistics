package com.logistics.logistics.trip.infrastructure.web;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.logistics.logistics.api.TripsApi;
import com.logistics.logistics.api.dto.TripDTO;
import com.logistics.logistics.api.dto.TripPageDTO;
import com.logistics.logistics.api.dto.TripRequestDTO;
import com.logistics.logistics.api.dto.TripStatusDTO;
import com.logistics.logistics.api.dto.TripStatusUpdateDTO;
import com.logistics.logistics.trip.application.TripService;

/** Rotas de viagens. Caminhos, validação e documentação vêm de {@link TripsApi}, gerada do contrato. */
@RestController
public class TripController implements TripsApi {

  private final TripService tripService;

  public TripController(TripService tripService) {
    this.tripService = tripService;
  }

  @Override
  public ResponseEntity<TripDTO> createTrip(TripRequestDTO request) {
    TripDTO created = TripApiMapper.toDto(tripService.createTrip(TripApiMapper.toCommand(request)));
    URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.getId()).toUri();
    return ResponseEntity.created(location).body(created);
  }

  @Override
  public ResponseEntity<TripPageDTO> listTrips(TripStatusDTO status, Integer page, Integer size) {
    return ResponseEntity.ok(TripApiMapper.toPage(tripService.listTrips(TripApiMapper.toDomain(status), page, size)));
  }

  @Override
  public ResponseEntity<TripDTO> getTrip(UUID id) {
    return ResponseEntity.ok(TripApiMapper.toDto(tripService.getTrip(id)));
  }

  @Override
  public ResponseEntity<TripDTO> updateTripStatus(UUID id, TripStatusUpdateDTO update) {
    return ResponseEntity.ok(TripApiMapper.toDto(tripService.updateStatus(id, TripApiMapper.toDomain(update.getStatus()))));
  }
}
