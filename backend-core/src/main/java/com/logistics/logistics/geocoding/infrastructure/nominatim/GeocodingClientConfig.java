package com.logistics.logistics.geocoding.infrastructure.nominatim;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import com.logistics.logistics.shared.config.HttpRequestFactories;

/** Cliente HTTP do Nominatim: URL base, User-Agent exigido pela política de uso e timeouts. */
@Configuration
class GeocodingClientConfig {

  @Bean
  RestClient geocodingRestClient(RestClient.Builder builder, GeocodingProperties properties) {
    return builder.clone()
        .baseUrl(properties.baseUrl())
        .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
        .requestFactory(HttpRequestFactories.withTimeouts(properties.connectTimeout(), properties.readTimeout()))
        .build();
  }
}
