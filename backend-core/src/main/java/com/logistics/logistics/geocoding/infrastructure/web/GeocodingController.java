package com.logistics.logistics.geocoding.infrastructure.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.logistics.api.GeocodingApi;
import com.logistics.logistics.api.dto.GeocodeResultDTO;
import com.logistics.logistics.geocoding.application.GeocodingService;
import com.logistics.logistics.geocoding.domain.GeoLocation;

/** Geocodificação. A busca ({@code q}) é dado pessoal: não vai para log (LGPD). */
@RestController
public class GeocodingController implements GeocodingApi {

  private final GeocodingService geocodingService;

  public GeocodingController(GeocodingService geocodingService) {
    this.geocodingService = geocodingService;
  }

  @Override
  public ResponseEntity<GeocodeResultDTO> geocode(String query) {
    GeoLocation location = geocodingService.geocode(query);
    return ResponseEntity.ok(new GeocodeResultDTO(location.displayName(), location.coordinates().lat(),
        location.coordinates().lng()));
  }
}
