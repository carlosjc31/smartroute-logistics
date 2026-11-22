package com.logistics.logistics.infrastructure.adapters.geocoding;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.logistics.logistics.application.exception.GeocodingUnavailableException;
import com.logistics.logistics.application.port.out.GeocodingPort;
import com.logistics.logistics.domain.Coordinates;
import com.logistics.logistics.domain.GeoLocation;

/**
 * Geocodificação pelo Nominatim (OpenStreetMap), respeitando a política de uso:
 * User-Agent identificável e no máximo uma requisição por intervalo configurado.
 */
@Component
public class NominatimGeocodingAdapter implements GeocodingPort {

  private static final ParameterizedTypeReference<List<NominatimPlace>> PLACES = new ParameterizedTypeReference<>() {};

  private final RestClient restClient;
  private final GeocodingProperties properties;
  private final Object throttleLock = new Object();
  private long lastCallNanos;

  public NominatimGeocodingAdapter(@Qualifier("geocodingRestClient") RestClient restClient,
      GeocodingProperties properties) {
    this.restClient = restClient;
    this.properties = properties;
  }

  @Override
  public Optional<GeoLocation> geocode(String query) {
    List<NominatimPlace> places;
    try {
      throttle();
      places = restClient.get()
          .uri(uri -> {
            uri.path("/search").queryParam("format", "json").queryParam("limit", 1).queryParam("q", query);
            if (properties.countryCodes() != null && !properties.countryCodes().isBlank())
              uri.queryParam("countrycodes", properties.countryCodes());
            return uri.build();
          })
          .retrieve()
          .body(PLACES);
    } catch (RestClientException e) {
      throw new GeocodingUnavailableException("Falha ao consultar o serviço de geocodificação", e);
    }
    if (places == null || places.isEmpty())
      return Optional.empty();
    NominatimPlace place = places.get(0);
    return Optional.of(new GeoLocation(place.displayName(),
        new Coordinates(Double.parseDouble(place.lat()), Double.parseDouble(place.lon()))));
  }

  private void throttle() {
    Duration interval = properties.minInterval();
    if (interval == null || interval.isZero())
      return;
    synchronized (throttleLock) {
      long waitNanos = lastCallNanos + interval.toNanos() - System.nanoTime();
      if (waitNanos > 0) {
        try {
          Thread.sleep(Duration.ofNanos(waitNanos));
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new GeocodingUnavailableException("Geocodificação interrompida", e);
        }
      }
      lastCallNanos = System.nanoTime();
    }
  }

  record NominatimPlace(String lat, String lon, @JsonProperty("display_name") String displayName) {}
}
