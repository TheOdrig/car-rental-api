package com.akif;

import com.akif.starter.CarGalleryProjectApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(CarGalleryProjectApplication.class);


    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    @Test
    void createsModuleDocumentation() {
        new Documenter(modules)
                .writeDocumentation()
                .writeIndividualModulesAsPlantUml();
    }
}
