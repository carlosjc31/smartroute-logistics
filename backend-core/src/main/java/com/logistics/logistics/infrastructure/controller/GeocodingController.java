package com.logistics.logistics.infrastructure.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.logistics.application.service.GeocodingService;
import com.logistics.logistics.infrastructure.controller.dto.GeocodeResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/geocode")
@Tag(name = "Geocodificação")
public class GeocodingController {

  private final GeocodingService geocodingService;

  public GeocodingController(GeocodingService geocodingService) {
    this.geocodingService = geocodingService;
  }

  @GetMapping
  @Operation(summary = "Converte um endereço ou cidade em coordenadas")
  public GeocodeResponseDTO geocode(@RequestParam("q") @NotBlank @Size(max = 200) String query) {
    return GeocodeResponseDTO.from(geocodingService.geocode(query));
  }
}
