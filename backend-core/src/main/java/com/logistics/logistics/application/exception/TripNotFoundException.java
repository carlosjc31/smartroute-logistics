package com.logistics.logistics.application.exception;

import java.util.UUID;

public class TripNotFoundException extends NotFoundException {
  public TripNotFoundException(UUID id) {
    super("Viagem não encontrada: " + id);
  }
}
