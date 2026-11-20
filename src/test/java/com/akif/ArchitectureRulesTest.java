package com.akif;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@DisplayName("Architecture rules")
class ArchitectureRulesTest {

    private static JavaClasses production;

    @BeforeAll
    static void importClasses() {
        production = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.akif");
    }

    @Test
    @DisplayName("web controllers do not depend on repositories")
    void webDoesNotUseRepositories() {
        noClasses().that().resideInAPackage("..web..")
                .should().dependOnClassesThat().resideInAPackage("..repository..")
                .check(production);
    }

    @Test
    @DisplayName("domain code does not import internal packages")
    void domainDoesNotImportInternal() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..internal..")
                .check(production);
    }

    @Test
    @DisplayName("api packages hold no JPA entities")
    void apiHasNoEntities() {
        noClasses().that().resideInAPackage("..api..")
                .should().beAnnotatedWith(Entity.class)
                .check(production);
    }

    @Test
    @DisplayName("ResponseEntity is used only by the web layer and the global exception handler")
    void responseEntityStaysInWeb() {
        noClasses().that().resideOutsideOfPackages("..web..", "com.akif.shared.handler..")
                .should().dependOnClassesThat().areAssignableTo(ResponseEntity.class)
                .check(production);
    }
}
