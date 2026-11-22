package com.logistics.logistics.domain.exception;

import com.logistics.logistics.domain.TripStatus;

public class InvalidStatusTransitionException extends DomainException {
  public InvalidStatusTransitionException(TripStatus from, TripStatus to) {
    super("Transição de status inválida: " + from + " → " + to);
  }
}
