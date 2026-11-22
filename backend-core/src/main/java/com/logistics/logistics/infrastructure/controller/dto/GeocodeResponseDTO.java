package com.logistics.logistics.infrastructure.controller.dto;

import com.logistics.logistics.domain.GeoLocation;

public record GeocodeResponseDTO(String displayName, double lat, double lng) {
  public static GeocodeResponseDTO from(GeoLocation location) {
    return new GeocodeResponseDTO(location.displayName(), location.coordinates().lat(), location.coordinates().lng());
  }
}
