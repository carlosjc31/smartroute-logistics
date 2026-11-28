package com.logistics.logistics.trip;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Uma viagem foi criada. Publicado na transação da criação; ouvintes com {@code @ApplicationModuleListener}
 * recebem depois do commit, pelo outbox. Sem coordenadas nem endereços (LGPD): quem precisar do detalhe
 * consulta o módulo de viagens.
 */
public record TripCreated(UUID tripId, String status, OffsetDateTime departureTime, OffsetDateTime occurredAt) {}
