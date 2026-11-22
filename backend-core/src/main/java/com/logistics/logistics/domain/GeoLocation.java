package com.logistics.logistics.domain;

/** Resultado de geocodificação: nome do lugar e suas coordenadas. */
public record GeoLocation(String displayName, Coordinates coordinates) {}
