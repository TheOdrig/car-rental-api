package com.akif.controller;

import com.akif.dto.request.CarSearchRequestDto;
import com.akif.dto.response.CarListResponseDto;
import com.akif.dto.response.CarResponseDto;
import com.akif.dto.response.CarSummaryResponseDto;
import com.akif.enums.CarStatusType;
import com.akif.enums.CurrencyType;
import com.akif.service.ICarService;
import com.akif.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CarSearchController.class)
@ContextConfiguration(classes = {CarSearchController.class, SecurityConfig.class})
@DisplayName("CarSearchController Unit Tests")
public class CarSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ICarService carService;

    @Autowired
    private ObjectMapper objectMapper;

    private CarSearchRequestDto testSearchRequest;
    private CarListResponseDto testSearchResponse;
    private CarResponseDto testCarResponseDto;

    @BeforeEach
    void setUp() {
        testCarResponseDto = CarResponseDto.builder()
                .id(1L)
                .licensePlate("34ABC123")
                .brand("Toyota")
                .model("Corolla")
                .productionYear(2020)
                .price(new BigDecimal("250000"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .build();

        testSearchRequest = CarSearchRequestDto.builder()
                .searchTerm("Toyota")
                .brand("Toyota")
                .minPrice(new BigDecimal("200000"))
                .maxPrice(new BigDecimal("300000"))
                .page(0)
                .size(10)
                .sortBy("price")
                .sortDirection("asc")
                .build();

        testSearchResponse = CarListResponseDto.builder()
                .cars(List.of(testCarResponseDto))
                .totalElements(1L)
                .totalPages(1)
                .currentPage(0)
                .pageSize(10)
                .numberOfElements(1)
                .build();
    }

    @Nested
    @DisplayName("POST /api/cars/search")
    class SearchCarsTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should search cars with valid criteria")
        void shouldSearchCarsWithValidCriteria() throws Exception {

            when(carService.searchCars(any(CarSearchRequestDto.class))).thenReturn(testSearchResponse);

            mockMvc.perform(post("/api/cars/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testSearchRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.cars").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.cars[0].brand").value("Toyota"));

            verify(carService).searchCars(any(CarSearchRequestDto.class));
        }
    }

    @Nested
    @DisplayName("GET /api/cars/search/status/{status}")
    class GetCarsByStatusTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return cars by status")
        void shouldReturnCarsByStatus() throws Exception {

            String status = "AVAILABLE";
            Page<CarResponseDto> carPage = new PageImpl<>(List.of(testCarResponseDto));
            when(carService.getCarsByStatus(eq(status), any())).thenReturn(carPage);

            mockMvc.perform(get("/api/cars/search/status/{status}", status)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].carStatusType").value("Available"));

            verify(carService).getCarsByStatus(eq(status), any());
        }
    }

    @Nested
    @DisplayName("GET /api/cars/search/brand/{brand}")
    class GetCarsByBrandTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return cars by brand")
        void shouldReturnCarsByBrand() throws Exception {

            String brand = "Toyota";
            Page<CarResponseDto> carPage = new PageImpl<>(List.of(testCarResponseDto));
            when(carService.getCarsByBrand(eq(brand), any())).thenReturn(carPage);

            mockMvc.perform(get("/api/cars/search/brand/{brand}", brand))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].brand").value(brand));

            verify(carService).getCarsByBrand(eq(brand), any());
        }
    }

    @Nested
    @DisplayName("GET /api/cars/search/price-range")
    class GetCarsByPriceRangeTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return cars by price range")
        void shouldReturnCarsByPriceRange() throws Exception {

            BigDecimal minPrice = new BigDecimal("200000");
            BigDecimal maxPrice = new BigDecimal("300000");
            Page<CarResponseDto> carPage = new PageImpl<>(List.of(testCarResponseDto));
            when(carService.getCarsByPriceRange(eq(minPrice), eq(maxPrice), any())).thenReturn(carPage);

            mockMvc.perform(get("/api/cars/search/price-range")
                            .param("minPrice", "200000")
                            .param("maxPrice", "300000"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray());

            verify(carService).getCarsByPriceRange(eq(minPrice), eq(maxPrice), any());
        }
    }

    @Nested
    @DisplayName("GET /api/cars/search/featured")
    class GetFeaturedCarsTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return featured cars")
        void shouldReturnFeaturedCars() throws Exception {

            Page<CarResponseDto> carPage = new PageImpl<>(List.of(testCarResponseDto));
            when(carService.getFeaturedCars(any())).thenReturn(carPage);

            mockMvc.perform(get("/api/cars/search/featured"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray());

            verify(carService).getFeaturedCars(any());
        }
    }

    @Nested
    @DisplayName("GET /api/cars/search/most-viewed")
    class GetMostViewedCarsTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return most viewed cars")
        void shouldReturnMostViewedCars() throws Exception {

            CarSummaryResponseDto summaryDto = CarSummaryResponseDto.builder()
                    .id(1L)
                    .licensePlate("34ABC123")
                    .brand("Toyota")
                    .model("Corolla")
                    .viewCount(100L)
                    .build();

            when(carService.getMostViewedCars(10)).thenReturn(List.of(summaryDto));

            mockMvc.perform(get("/api/cars/search/most-viewed")
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].viewCount").value(100));

            verify(carService).getMostViewedCars(10);
        }
    }

}
