package com.akif;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ModularityTests {

    private static final Set<String> EXPECTED_MODULES =
            Set.of("auth", "car", "currency", "damage", "dashboard", "notification", "payment", "rental", "shared");

    private final ApplicationModules modules = ApplicationModules.of(CarRentalApplication.class);

    @Test
    void detectsExactlyTheExpectedModules() {
        Set<String> detected = modules.stream()
                .map(ApplicationModule::getIdentifier)
                .map(Object::toString)
                .collect(Collectors.toSet());

        assertThat(detected).isEqualTo(EXPECTED_MODULES);
    }

    @Test
    void createsModuleDocumentation() {
        new Documenter(modules)
                .writeDocumentation()
                .writeIndividualModulesAsPlantUml();
    }
}
