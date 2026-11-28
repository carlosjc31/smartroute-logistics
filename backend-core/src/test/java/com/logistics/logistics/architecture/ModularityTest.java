package com.logistics.logistics.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import com.logistics.logistics.BackendCoreApplication;

/**
 * Fronteiras do monólito modular (ADR 0004): sem ciclos entre módulos e sem acesso a pacote interno de
 * outro módulo. Só o pacote raiz de cada módulo é API pública; {@code shared} e {@code api} são abertos.
 */
class ModularityTest {

  private static final ApplicationModules MODULES = ApplicationModules.of(BackendCoreApplication.class);

  @Test
  void modulesRespectTheirBoundaries() {
    MODULES.verify();
  }

  /** Diagramas (PlantUML) e fichas dos módulos em target/spring-modulith-docs. */
  @Test
  void writesModuleDocumentation() {
    new Documenter(MODULES).writeDocumentation();
  }
}
