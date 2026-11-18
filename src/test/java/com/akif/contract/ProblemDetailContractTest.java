package com.akif.contract;

import com.akif.CarRentalApplication;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("RFC 9457 problem details")
class ProblemDetailContractTest {

    private static final List<String> STANDARD_MEMBERS = List.of("type", "title", "status", "detail", "instance",
            "code", "timestamp");

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper mapper = JsonMapper.builder().build();

    private ResultActions assertProblem(ResultActions result, int status, String code) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(startsWith("urn:car-rental:problem:")))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").isNotEmpty())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
        JsonNode body = mapper.readTree(result.andReturn().getResponse().getContentAsString());
        List<String> missing = new ArrayList<>();
        STANDARD_MEMBERS.stream().filter(member -> !body.has(member)).forEach(missing::add);
        assertThat(missing).isEmpty();
        return result;
    }

    @Test
    @DisplayName("a missing token is a 401 problem")
    void unauthenticated() throws Exception {
        assertProblem(mockMvc.perform(get("/api/v1/users/me/profile")), 401, "AUTHENTICATION_FAILED")
                .andExpect(jsonPath("$.instance").value("/api/v1/users/me/profile"));
    }

    @Test
    @DisplayName("a missing role is a 403 problem")
    void forbidden() throws Exception {
        assertProblem(mockMvc.perform(get("/api/v1/admin/dashboard/summary")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))), 403, "ACCESS_DENIED");
    }

    @Test
    @DisplayName("an unknown endpoint is a 404 problem")
    void unknownEndpoint() throws Exception {
        assertProblem(mockMvc.perform(get("/api/v1/does-not-exist")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))), 404, "ENDPOINT_NOT_FOUND");
    }

    @Test
    @DisplayName("a wrong method is a 405 problem")
    void methodNotAllowed() throws Exception {
        assertProblem(mockMvc.perform(post("/api/v1/cars/1")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN")))),
                405, "METHOD_NOT_ALLOWED");
    }

    @Test
    @DisplayName("a malformed body is a 400 problem")
    void malformedJson() throws Exception {
        assertProblem(mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{not json")), 400, "INVALID_JSON");
    }

    @Test
    @DisplayName("an invalid body lists field errors with real field names")
    void validationErrors() throws Exception {
        assertProblem(mockMvc.perform(post("/api/v1/cars/availability/search")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startDate\":\"2030-01-10\",\"endDate\":\"2030-01-05\"}")),
                400, "VALIDATION_FAILED")
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("endDate")))
                .andExpect(jsonPath("$.errors[?(@.field == 'endDate')].message")
                        .value(hasItem("End date must be after start date")));

        String body = mockMvc.perform(post("/api/v1/rentals/request")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carId\":1,\"startDate\":\"2030-01-10\",\"endDate\":\"2030-01-05\"}"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("endDateAfterStartDate").doesNotContain("withinMax");
    }

    @Test
    @DisplayName("a missing webhook signature is a 400 problem")
    void webhookWithoutSignature() throws Exception {
        assertProblem(mockMvc.perform(post("/api/v1/webhooks/stripe").contentType(MediaType.APPLICATION_JSON)
                .content("{}")), 400, "WEBHOOK_SIGNATURE_MISSING");
    }

    @Test
    @DisplayName("an invalid webhook signature is a 400 problem")
    void webhookWithBadSignature() throws Exception {
        assertProblem(mockMvc.perform(post("/api/v1/webhooks/stripe").contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", "t=1,v1=bad").content("{}")), 400, "WEBHOOK_SIGNATURE_INVALID");
    }
}
