package com.logistics.logistics.shared.config;

import java.time.Clock;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
@EnableCaching
// Ouvintes @ApplicationModuleListener rodam depois do commit, em outra thread; o outbox (events.event_publication)
// guarda a publicação até o ouvinte concluir
@EnableAsync
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
        .description("Despacho de viagens com previsão de chegada (ETA) por machine learning."))
        // Token do Keycloak no botão "Authorize" do Swagger UI
        .components(new Components().addSecuritySchemes("bearer", new SecurityScheme()
            .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList("bearer"));
  }
}
