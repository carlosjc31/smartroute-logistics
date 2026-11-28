package com.logistics.logistics.trip.domain;

import java.util.EnumSet;
import java.util.Set;

/** Ciclo de vida de uma viagem e as transições permitidas entre os estados. */
public enum TripStatus {
  PENDING,
  IN_TRANSIT,
  DELAYED,
  COMPLETED,
  CANCELLED;

  public Set<TripStatus> allowedTransitions() {
    return switch (this) {
      case PENDING -> EnumSet.of(IN_TRANSIT, CANCELLED);
      case IN_TRANSIT -> EnumSet.of(DELAYED, COMPLETED);
      case DELAYED -> EnumSet.of(COMPLETED);
      case COMPLETED, CANCELLED -> EnumSet.noneOf(TripStatus.class);
    };
  }

  public boolean canTransitionTo(TripStatus target) {
    return allowedTransitions().contains(target);
  }

  public boolean isFinal() {
    return allowedTransitions().isEmpty();
  }
}
