package com.logistics.logistics.infrastructure.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.logistics.application.services.TripService;
import com.logistics.logistics.domain.Trip;
import com.logistics.logistics.infrastructure.controller.dto.TripRequestDTO;
import com.logistics.logistics.infrastructure.persistence.TripEntity;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripController {
private final TripService tripService;

    @PostMapping
    public ResponseEntity<TripEntity> createTrip(@Valid @RequestBody TripRequestDTO dto) {
        Trip trip = dto.toDomain();

        TripEntity savedTrip = tripService.createNewTrip(trip);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedTrip);
    }
}
