package io.github.erf88.stackemu;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
  private static com.tngtech.archunit.core.domain.JavaClasses productionClasses() {
    return new ClassFileImporter()
        .withImportOption(new ImportOption.DoNotIncludeTests())
        .importPackages("io.github.erf88.stackemu");
  }

  @Test
  void awsAdaptersCannotDependOnWebPresentation() {
    noClasses()
        .that()
        .resideInAPackage("..adapter.aws..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..adapter.web..", "io.quarkus.qute..", "jakarta.ws.rs..")
        .check(productionClasses());
  }

  @Test
  void roleNamesMatchTheirPackageResponsibilities() {
    var production = productionClasses();
    classes()
        .that()
        .areAnnotatedWith(Path.class)
        .should()
        .haveSimpleNameEndingWith("Resource")
        .andShould()
        .resideInAPackage("..adapter.web..")
        .check(production);
    classes()
        .that()
        .areInterfaces()
        .and()
        .resideInAPackage("..application..")
        .should()
        .haveSimpleNameEndingWith("Port")
        .check(production);
    classes()
        .that()
        .haveSimpleNameEndingWith("Adapter")
        .should()
        .resideInAnyPackage("..adapter.aws..", "..adapter.persistence..")
        .check(production);
  }

  @Test
  void productionIdentifiersAndInjectionFollowConventions() {
    for (var type : productionClasses()) {
      assertTrue(
          type.getPackageName().matches("io\\.github\\.erf88\\.stackemu(?:\\.[a-z][a-z0-9]*)+"),
          type.getName());
      if (!type.getName().contains("$")) {
        assertTrue(type.getSimpleName().matches("[A-Z][A-Za-z0-9]*"), type.getName());
      }
      for (var method : type.getMethods()) {
        if (!method.reflect().isSynthetic()) {
          assertTrue(method.getName().matches("[a-z][A-Za-z0-9]*"), method.getFullName());
        }
      }
      for (var field : type.getFields()) {
        assertFalse(
            field.isAnnotatedWith(Inject.class),
            "Use constructor injection: " + field.getFullName());
      }
    }
  }

  @Test
  void applicationContractsStayIndependent() {
    var classes =
        new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("io.github.erf88.stackemu");
    noClasses()
        .that()
        .resideInAPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("io.quarkus..", "jakarta..", "software.amazon..", "..adapter..")
        .check(classes);
    noClasses()
        .that()
        .resideInAPackage("..adapter.web..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("software.amazon..", "..adapter.aws..")
        .check(classes);
  }
}
