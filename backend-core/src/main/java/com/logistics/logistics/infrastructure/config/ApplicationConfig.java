package com.logistics.logistics.infrastructure.config;

import java.time.Clock;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
@EnableCaching
public class ApplicationConfig {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  OpenAPI smartRouteOpenApi() {
    return new OpenAPI().info(new Info()
        .title("SmartRoute Logistics API")
        .version("v1")
        .description("Despacho de viagens com previsão de chegada (ETA) por machine learning."));
  }
}
