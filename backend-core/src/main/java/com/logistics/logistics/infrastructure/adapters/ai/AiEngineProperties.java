package com.logistics.logistics.infrastructure.adapters.ai;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai-engine")
public record AiEngineProperties(String url, Duration connectTimeout, Duration readTimeout) {}
