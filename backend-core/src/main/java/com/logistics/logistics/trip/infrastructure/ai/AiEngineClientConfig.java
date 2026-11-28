package com.logistics.logistics.trip.infrastructure.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import com.logistics.logistics.shared.config.HttpRequestFactories;

/** Cliente HTTP do ai-engine: URL base, token de serviço e timeouts. */
@Configuration
class AiEngineClientConfig {

  @Bean
  RestClient aiEngineRestClient(RestClient.Builder builder, AiEngineProperties properties) {
    return builder.clone()
        .baseUrl(properties.url())
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.token())
        .requestFactory(HttpRequestFactories.withTimeouts(properties.connectTimeout(), properties.readTimeout()))
        .build();
  }
}
