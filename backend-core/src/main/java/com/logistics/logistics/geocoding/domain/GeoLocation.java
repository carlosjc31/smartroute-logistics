package com.logistics.logistics.geocoding.domain;

import com.logistics.logistics.shared.domain.Coordinates;

/** Resultado de geocodificação: nome do lugar e suas coordenadas. */
public record GeoLocation(String displayName, Coordinates coordinates) {}
