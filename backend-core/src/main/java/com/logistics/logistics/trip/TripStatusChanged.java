package com.logistics.logistics.trip;

import java.time.OffsetDateTime;
import java.util.UUID;

/** O status de uma viagem mudou (por exemplo IN_TRANSIT → DELAYED). Sem dados pessoais. */
public record TripStatusChanged(UUID tripId, String previousStatus, String status, OffsetDateTime occurredAt) {}
