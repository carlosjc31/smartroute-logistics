package com.logistics.logistics.infrastructure.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.logistics.logistics.application.service.TripService;
import com.logistics.logistics.domain.TripStatus;
import com.logistics.logistics.infrastructure.controller.dto.PageResponseDTO;
import com.logistics.logistics.infrastructure.controller.dto.TripRequestDTO;
import com.logistics.logistics.infrastructure.controller.dto.TripResponseDTO;
import com.logistics.logistics.infrastructure.controller.dto.TripStatusUpdateDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/trips")
@Tag(name = "Viagens")
public class TripController {

  private final TripService tripService;

  public TripController(TripService tripService) {
    this.tripService = tripService;
  }

  @PostMapping
  @Operation(summary = "Cria uma viagem e calcula a previsão de chegada")
  public ResponseEntity<TripResponseDTO> createTrip(@Valid @RequestBody TripRequestDTO dto) {
    TripResponseDTO created = TripResponseDTO.from(tripService.createTrip(dto.toCommand()));
    URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
    return ResponseEntity.created(location).body(created);
  }

  @GetMapping
  @Operation(summary = "Lista viagens (mais recentes primeiro), com filtro opcional por status")
  public PageResponseDTO<TripResponseDTO> listTrips(
      @RequestParam(required = false) TripStatus status,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return PageResponseDTO.from(tripService.listTrips(status, page, size), TripResponseDTO::from);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Detalha uma viagem")
  public TripResponseDTO getTrip(@PathVariable UUID id) {
    return TripResponseDTO.from(tripService.getTrip(id));
  }

  @PatchMapping("/{id}/status")
  @Operation(summary = "Avança o status da viagem (ver allowedTransitions)")
  public TripResponseDTO updateStatus(@PathVariable UUID id, @Valid @RequestBody TripStatusUpdateDTO dto) {
    return TripResponseDTO.from(tripService.updateStatus(id, dto.status()));
  }
}
