/**
 * Viagens: criação com previsão de chegada (ai-engine ou heurística), ciclo de vida e consultas (spec 001).
 * API pública: os eventos {@link com.logistics.logistics.trip.TripCreated} e
 * {@link com.logistics.logistics.trip.TripStatusChanged}. Dono do schema {@code trip}.
 */
@ApplicationModule(displayName = "Viagens")
package com.logistics.logistics.trip;

import org.springframework.modulith.ApplicationModule;
