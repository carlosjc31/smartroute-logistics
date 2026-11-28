package com.logistics.logistics.trip.domain;

import com.logistics.logistics.shared.domain.DomainException;

public class InvalidStatusTransitionException extends DomainException {
  public InvalidStatusTransitionException(TripStatus from, TripStatus to) {
    super("Transição de status inválida: " + from + " → " + to);
  }
}
