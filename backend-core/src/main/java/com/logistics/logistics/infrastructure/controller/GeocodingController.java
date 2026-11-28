package com.logistics.logistics.infrastructure.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.logistics.application.service.GeocodingService;
import com.logistics.logistics.infrastructure.controller.api.GeocodingApi;
import com.logistics.logistics.infrastructure.controller.dto.GeocodeResultDTO;
import com.logistics.logistics.infrastructure.controller.mapper.ApiMapper;

/** Geocodificação. A busca ({@code q}) é dado pessoal: não vai para log (LGPD). */
@RestController
public class GeocodingController implements GeocodingApi {

  private final GeocodingService geocodingService;

  public GeocodingController(GeocodingService geocodingService) {
    this.geocodingService = geocodingService;
  }

  @Override
  public ResponseEntity<GeocodeResultDTO> geocode(String query) {
    return ResponseEntity.ok(ApiMapper.toDto(geocodingService.geocode(query)));
  }
}
