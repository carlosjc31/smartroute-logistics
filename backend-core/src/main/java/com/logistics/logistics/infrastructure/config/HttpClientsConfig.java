package com.logistics.logistics.infrastructure.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.logistics.logistics.infrastructure.adapters.ai.AiEngineProperties;
import com.logistics.logistics.infrastructure.adapters.geocoding.GeocodingProperties;

/** Clientes HTTP de saída, cada um com sua URL base e timeouts. */
@Configuration
public class HttpClientsConfig {

  @Bean
  RestClient aiEngineRestClient(RestClient.Builder builder, AiEngineProperties properties) {
    return builder.clone()
        .baseUrl(properties.url())
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.token())
        .requestFactory(requestFactory(properties.connectTimeout(), properties.readTimeout()))
        .build();
  }

  @Bean
  RestClient geocodingRestClient(RestClient.Builder builder, GeocodingProperties properties) {
    return builder.clone()
        .baseUrl(properties.baseUrl())
        .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
        .requestFactory(requestFactory(properties.connectTimeout(), properties.readTimeout()))
        .build();
  }

  private static SimpleClientHttpRequestFactory requestFactory(Duration connect, Duration read) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(connect);
    factory.setReadTimeout(read);
    return factory;
  }
}
