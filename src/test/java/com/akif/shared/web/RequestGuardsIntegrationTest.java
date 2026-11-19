package com.akif.shared.web;

import com.akif.CarRentalApplication;
import com.akif.shared.handler.GlobalExceptionHandler;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Request guards: paging, sorting, body size, path types")
class RequestGuardsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GlobalExceptionHandler exceptionHandler;

    @Test
    @DisplayName("an unknown sort field answers 400 INVALID_PARAMETER")
    void unknownSortIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/cars/active").param("sort", "passwordHash,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("an allowed sort field is accepted")
    void allowedSortIsAccepted() throws Exception {
        mockMvc.perform(get("/api/v1/cars/active").param("sort", "price,desc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("size and page outside their bounds answer 400 instead of falling back silently")
    void invalidPagingIsBadRequest() throws Exception {
        for (String size : List.of("0", "-1", "abc", "101")) {
            mockMvc.perform(get("/api/v1/cars/active").param("size", size))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        }
        mockMvc.perform(get("/api/v1/cars/active").param("page", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/cars/active").param("size", "100"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("a non-numeric car id answers 400 for anonymous callers")
    void nonNumericCarIdIsBadRequestForAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/cars/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_TYPE"));
    }

    @Test
    @DisplayName("a JSON body above the limit answers 413 as a problem document")
    void oversizedJsonBodyIsRejected() throws Exception {
        String big = "{\"username\":\"" + "a".repeat(2 * 1024 * 1024) + "\"}";
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(big))
                .andExpect(status().isContentTooLarge())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("PAYLOAD_TOO_LARGE"));
    }

    @Test
    @DisplayName("a JSON body within the limit is still parsed")
    void normalJsonBodyIsAccepted() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nobody\",\"password\":\"x\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("a bodiless POST to an action endpoint is not rejected for the missing content type")
    void bodilessPostIsNotUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/api/v1/rentals/1/cancel"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "ADMIN")
    @DisplayName("admin action endpoints accept POSTs without a body or content type")
    void adminActionsAcceptBodilessPosts() throws Exception {
        mockMvc.perform(post("/api/v1/admin/damages/999999/charge")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/rentals/999999/confirm")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a data access sort error caused by an unknown property maps to 400")
    void propertyReferenceMapsToBadRequest() {
        PropertyReferenceException cause = new PropertyReferenceException("nope",
                TypeInformation.of(String.class), List.of());
        var response = exceptionHandler.handleInvalidDataAccessUsage(
                new InvalidDataAccessApiUsageException("bad", cause),
                new ServletWebRequest(new MockHttpServletRequest()));
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }
}
