package com.akif.car.integration;

import com.akif.CarRentalApplication;
import com.akif.car.api.CarStatusType;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.shared.enums.CurrencyType;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Car search applies every accepted filter")
class CarSearchFiltersIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CarRepository carRepository;

    @BeforeEach
    void setUp() {
        carRepository.deleteAll();
        save("34AAA111", "Toyota", 5, 5, 20000L, "4.5", "Petrol", "Red");
        save("34BBB222", "Honda", 2, 2, 90000L, "3.0", "Diesel", "Blue");
        save("34CCC333", "BMW", 5, 7, 5000L, null, "Petrol", "Black");
    }

    @AfterEach
    void tearDown() {
        carRepository.deleteAll();
    }

    private void save(String plate, String brand, int doors, int seats, long kilometer, String rating,
                      String engineType, String color) {
        Car car = new Car();
        car.setLicensePlate(plate);
        car.setVinNumber("VIN" + plate);
        car.setBrand(brand);
        car.setModel("M" + plate);
        car.setProductionYear(2021);
        car.setPrice(new BigDecimal("1000"));
        car.setCurrencyType(CurrencyType.TRY);
        car.setCarStatusType(CarStatusType.AVAILABLE);
        car.setDoors(doors);
        car.setSeats(seats);
        car.setKilometer(kilometer);
        car.setRating(rating == null ? null : new BigDecimal(rating));
        car.setEngineType(engineType);
        car.setColor(color);
        carRepository.save(car);
    }

    @Test
    @DisplayName("minRating keeps only cars rated at or above it")
    void minRating() throws Exception {
        mockMvc.perform(get("/api/v1/cars/search").param("minRating", "4.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].licensePlate").value("34AAA111"));
    }

    @Test
    @DisplayName("maxSeats and minDoors narrow by capacity and body")
    void seatsAndDoors() throws Exception {
        mockMvc.perform(get("/api/v1/cars/search").param("maxSeats", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/v1/cars/search").param("minDoors", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("maxKilometer keeps only cars with at most that mileage")
    void maxKilometer() throws Exception {
        mockMvc.perform(get("/api/v1/cars/search").param("maxKilometer", "20000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("engineType and color match case-insensitively")
    void engineTypeAndColor() throws Exception {
        mockMvc.perform(get("/api/v1/cars/search").param("engineType", "diesel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].licensePlate").value("34BBB222"));
        mockMvc.perform(get("/api/v1/cars/search").param("color", "BLACK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].licensePlate").value("34CCC333"));
    }

    @Test
    @DisplayName("filters combine and the rating bounds are validated")
    void combinedAndValidated() throws Exception {
        mockMvc.perform(get("/api/v1/cars/search").param("engineType", "petrol").param("minSeats", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/cars/search").param("minRating", "0.5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("catalogue search is public and has no POST variant")
    void publicWithoutPost() throws Exception {
        mockMvc.perform(get("/api/v1/cars/search"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/cars/search").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is4xxClientError());
    }

    @ParameterizedTest
    @CsvSource({
            "maxKilometer,1000000",
            "minDoors,1",
            "maxSeats,10",
            "minSeats,0",
            "minRating,0.5",
            "minPrice,-1",
            "size,101"
    })
    @DisplayName("out-of-range filter values are rejected")
    void rejectsOutOfRange(String name, String value) throws Exception {
        mockMvc.perform(get("/api/v1/cars/search").param(name, value))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({
            "maxKilometer,999999,3",
            "maxKilometer,0,0",
            "minDoors,2,3",
            "minDoors,6,0",
            "maxSeats,9,3",
            "maxSeats,1,0",
            "minRating,1.0,2",
            "minRating,4.5,1",
            "minRating,5.0,0",
            "engineType,PETROL,2",
            "engineType,electric,0",
            "color,red,1",
            "color,green,0"
    })
    @DisplayName("boundary filter values return the expected number of cars")
    void boundaryValues(String name, String value, int expected) throws Exception {
        mockMvc.perform(get("/api/v1/cars/search").param(name, value))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(expected));
    }
}
