package com.akif.shared.security;

import com.akif.CarRentalApplication;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class, properties = {
        "springdoc.swagger-ui.enabled=false",
        "springdoc.api-docs.enabled=false"
})
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Swagger UI entry path with the UI disabled")
class SwaggerEntryDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("stays behind authentication when the UI is switched off")
    void entryPathIsNotPermitted() throws Exception {
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isUnauthorized());
    }
}
