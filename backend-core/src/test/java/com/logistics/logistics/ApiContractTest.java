package com.logistics.logistics;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

import com.logistics.logistics.audit.AuditService;
import com.logistics.logistics.audit.infrastructure.web.AuditController;
import com.logistics.logistics.geocoding.application.GeocodingService;
import com.logistics.logistics.geocoding.infrastructure.web.GeocodingController;
import com.logistics.logistics.shared.security.SecurityConfig;
import com.logistics.logistics.trip.application.TripService;
import com.logistics.logistics.trip.infrastructure.web.TripController;

/**
 * Contract-first (ADR 0009): as rotas que o backend publica são exatamente as do contrato. As interfaces
 * geradas garantem que toda operação do contrato existe; este teste pega a rota escrita à mão fora dele.
 */
@WebMvcTest({TripController.class, GeocodingController.class, AuditController.class})
@Import(SecurityConfig.class)
class ApiContractTest {

  private static final Path CONTRACT = Path.of("..", "contracts", "api", "openapi.yaml");
  private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete");

  @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping;
  @MockBean TripService tripService;
  @MockBean GeocodingService geocodingService;
  @MockBean AuditService auditService;

  @Test
  void publishedRoutesAreExactlyTheContractOperations() throws IOException {
    assertThat(publishedRoutes()).isEqualTo(contractRoutes());
  }

  private Set<String> publishedRoutes() {
    Set<String> routes = new TreeSet<>();
    handlerMapping.getHandlerMethods().forEach((info, method) -> {
      if (!method.getBeanType().getPackageName().startsWith(getClass().getPackageName())) {
        return;
      }
      for (String path : info.getPatternValues()) {
        info.getMethodsCondition().getMethods().forEach(verb -> routes.add(verb.name() + " " + path));
      }
    });
    return routes;
  }

  @SuppressWarnings("unchecked")
  private static Set<String> contractRoutes() throws IOException {
    Map<String, Object> contract;
    try (InputStream in = Files.newInputStream(CONTRACT)) {
      contract = new Yaml().load(in);
    }
    Set<String> routes = new TreeSet<>();
    ((Map<String, Map<String, Object>>) contract.get("paths")).forEach((path, operations) ->
        operations.keySet().stream().filter(HTTP_METHODS::contains)
            .forEach(verb -> routes.add(verb.toUpperCase() + " " + path)));
    return routes;
  }
}
