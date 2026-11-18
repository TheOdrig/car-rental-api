package com.akif.contract;

import com.akif.CarRentalApplication;
import com.akif.testsupport.TestcontainersConfiguration;
import com.akif.shared.web.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Paged responses")
class PageResponseContractTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("maps a Spring Data page to the stable DTO")
    void mapsPage() {
        PageResponse<String> response = PageResponse.of(
                new PageImpl<>(List.of("a", "b"), PageRequest.of(2, 2), 7));

        assertThat(response.content()).containsExactly("a", "b");
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(7);
        assertThat(response.totalPages()).isEqualTo(4);
    }

    @Test
    @DisplayName("list endpoints expose the PageResponse shape only")
    void pageShape() throws Exception {
        String body = mockMvc.perform(get("/api/v1/cars"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = mapper.readTree(body);
        List<String> names = new ArrayList<>();
        json.propertyNames().forEach(names::add);

        assertThat(names).containsExactlyInAnyOrder("content", "page", "size", "totalElements", "totalPages");
        assertThat(json.get("content").isArray()).isTrue();
        assertThat(json.get("page").asInt()).isZero();
    }
}
