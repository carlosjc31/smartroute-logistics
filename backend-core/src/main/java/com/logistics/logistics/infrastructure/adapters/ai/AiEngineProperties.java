package com.logistics.logistics.infrastructure.adapters.ai;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** @param token token de serviço exigido pelo ai-engine (header Authorization: Bearer) */
@ConfigurationProperties(prefix = "ai-engine")
public record AiEngineProperties(String url, String token, Duration connectTimeout, Duration readTimeout) {

  public AiEngineProperties {
    if (token == null || token.isBlank())
      throw new IllegalArgumentException("ai-engine.token (AI_ENGINE_TOKEN) é obrigatório");
  }
}
