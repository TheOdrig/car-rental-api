package com.akif.service;

import com.akif.dto.request.CarPriceUpdateRequestDto;
import com.akif.dto.request.CarRequestDto;
import com.akif.dto.request.CarSearchRequestDto;
import com.akif.dto.request.CarStatusUpdateRequestDto;
import com.akif.dto.response.CarListResponseDto;
import com.akif.dto.response.CarResponseDto;
import com.akif.dto.response.CarSummaryResponseDto;
import com.akif.shared.enums.CarStatusType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ICarService {

    CarResponseDto getCarById(Long id);

    CarResponseDto getCarByLicensePlate(String licensePlate);

    CarResponseDto createCar(CarRequestDto carRequest);

    CarResponseDto updateCar(Long id, CarRequestDto carRequest);

    void deleteCar(Long id);

    void softDeleteCar(Long id);

    void restoreCar(Long id);


    CarListResponseDto searchCars(CarSearchRequestDto searchRequest);
    Page<CarResponseDto> getAllCars(Pageable pageable);

    Page<CarResponseDto> getCarsByStatus(String status, Pageable pageable);
    long getCarCount();

    Page<CarResponseDto> getCarsByBrand(String brand, Pageable pageable);

    Page<CarResponseDto> getCarsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    Page<CarResponseDto> getNewCars(Pageable pageable);

    Page<CarResponseDto> getFeaturedCars(Pageable pageable);

    Page<CarResponseDto> getCarsAvailableForTestDrive(Pageable pageable);

    Page<CarResponseDto> getAllActiveCars(Pageable pageable);
    long getActiveCarCount();


    CarResponseDto sellCar(Long id);

    CarResponseDto reserveCar(Long id);

    CarResponseDto cancelReservation(Long id);

    CarResponseDto markAsMaintenance(Long id);

    CarResponseDto markAsAvailable(Long id);

    CarResponseDto updateCarStatus(Long id, CarStatusUpdateRequestDto statusUpdateRequest);

    CarResponseDto updateCarPrice(Long id, CarPriceUpdateRequestDto priceUpdateRequest);


    Map<String, Object> getCarStatistics();

    Map<String, Long> getCarsCountByStatus();

    Map<String, Long> getCarsCountByBrand();

    Map<String, BigDecimal> getAveragePriceByBrand();

    List<CarSummaryResponseDto> getMostViewedCars(int limit);

    List<CarSummaryResponseDto> getMostLikedCars(int limit);


    boolean existsById(Long id);

    boolean existsByLicensePlate(String licensePlate);

    void incrementViewCount(Long id);

    void incrementLikeCount(Long id);

    void decrementLikeCount(Long id);


    List<String> validateCarData(CarRequestDto carRequest);

    boolean canCarBeSold(Long id);

    boolean canCarBeReserved(Long id);

    Page<CarResponseDto> searchCarsByCriteria(String searchTerm, String brand, String model,
                                    BigDecimal minPrice, BigDecimal maxPrice,
                                    CarStatusType status, Pageable pageable);
}
