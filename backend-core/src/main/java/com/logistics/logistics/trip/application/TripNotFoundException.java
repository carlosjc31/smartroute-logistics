package com.logistics.logistics.trip.application;

import java.util.UUID;

import com.logistics.logistics.shared.application.NotFoundException;

public class TripNotFoundException extends NotFoundException {
  public TripNotFoundException(UUID id) {
    super("Viagem não encontrada: " + id);
  }
}
