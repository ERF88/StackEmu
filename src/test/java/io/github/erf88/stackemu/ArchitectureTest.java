package io.github.erf88.stackemu;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
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
