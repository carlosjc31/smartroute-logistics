package com.logistics.logistics.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Regras hexagonais dentro de cada módulo (ADR 0003): o domínio não conhece framework, e a aplicação não
 * conhece adapters. O Modulith cuida das fronteiras entre módulos ({@link ModularityTest}).
 */
class HexagonalArchitectureTest {

  private static final JavaClasses CLASSES = new ClassFileImporter()
      .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
      .importPackages("com.logistics.logistics");

  @Test
  void domainHasNoFrameworkDependencies() {
    noClasses().that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "org.springframework..", "jakarta.persistence..", "jakarta.validation..", "com.fasterxml.jackson..",
            "org.hibernate..", "..application..", "..infrastructure..", "com.logistics.logistics.api..")
        .because("o domínio é Java puro (ADR 0003)")
        .check(CLASSES);
  }

  @Test
  void applicationDoesNotDependOnAdapters() {
    noClasses().that().resideInAPackage("..application..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "..infrastructure..", "com.logistics.logistics.api..", "jakarta.persistence..",
            "org.springframework.web..", "org.springframework.security..", "org.springframework.jdbc..")
        .because("casos de uso falam com o mundo por portas; os adapters ficam em infrastructure")
        .check(CLASSES);
  }

  @Test
  void onlyInfrastructureUsesTheGeneratedApi() {
    noClasses().that().resideOutsideOfPackages("..infrastructure..", "com.logistics.logistics.api..")
        .should().dependOnClassesThat().resideInAPackage("com.logistics.logistics.api..")
        .because("os DTOs do contrato não vazam para o domínio nem para os casos de uso")
        .check(CLASSES);
  }
}
