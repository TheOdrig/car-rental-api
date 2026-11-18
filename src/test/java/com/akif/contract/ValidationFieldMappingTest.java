package com.akif.contract;

import com.akif.shared.handler.ReportsAsField;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

@DisplayName("Validation error field mapping")
class ValidationFieldMappingTest {

    @Test
    @DisplayName("method level rules declare the request field they report under")
    void assertTrueMethodsNameTheirField() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.akif");

        methods().that().areAnnotatedWith(AssertTrue.class)
                .should().beAnnotatedWith(ReportsAsField.class)
                .because("clients must see a request field name, not a getter name")
                .check(classes);
    }

    @Test
    @DisplayName("method level rules stay out of JSON and the OpenAPI schema")
    void assertTrueMethodsAreHidden() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.akif");

        methods().that().areAnnotatedWith(AssertTrue.class)
                .should().beAnnotatedWith(JsonIgnore.class)
                .andShould().beAnnotatedWith(Schema.class)
                .because("helper booleans are validation plumbing, not part of the request contract")
                .check(classes);
    }
}
