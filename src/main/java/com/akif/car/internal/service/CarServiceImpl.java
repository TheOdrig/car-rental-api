package com.akif.car.internal.service;

import com.akif.car.api.CarDto;
import com.akif.car.api.CarResponse;
import com.akif.shared.web.PageResponse;
import com.akif.car.api.CarService;
import com.akif.car.api.CarStatusType;
import com.akif.car.api.FilterOptionsResponse;
import com.akif.car.internal.dto.pricing.CarPriceUpdateRequest;
import com.akif.car.internal.dto.request.CarRequest;
import com.akif.car.internal.dto.request.CarSearchRequest;
import com.akif.car.internal.dto.request.CarStatusUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CarServiceImpl implements CarService {

    private final CarQueryService queryService;
    private final CarCommandService commandService;
    private final CarStatusService statusService;
    private final CarStatisticsService statisticsService;

    @Override
    public CarDto getCarDtoById(Long id) {
        return queryService.getCarDtoById(id);
    }

    @Override
    public CarResponse getCarById(Long id) {
        return queryService.getCarById(id);
    }

    @Override
    public CarResponse getCarForBooking(Long id) {
        return queryService.getCarForBooking(id);
    }

    @Override
    public CarResponse getCarByLicensePlate(String licensePlate) {
        return queryService.getCarByLicensePlate(licensePlate);
    }

    @Override
    public CarResponse createCar(CarRequest carRequest) {
        return commandService.createCar(carRequest);
    }

    @Override
    public CarResponse updateCar(Long id, CarRequest carRequest) {
        return commandService.updateCar(id, carRequest);
    }

    @Override
    public void deleteCar(Long id) {
        commandService.deleteCar(id);
    }

    @Override
    public void softDeleteCar(Long id) {
        commandService.softDeleteCar(id);
    }

    @Override
    public CarResponse restoreCar(Long id) {
        return commandService.restoreCar(id);
    }

    @Override
    public CarResponse updateCarPrice(Long id, CarPriceUpdateRequest priceUpdateRequest) {
        return commandService.updateCarPrice(id, priceUpdateRequest);
    }

    @Override
    public List<String> validateCarData(CarRequest carRequest) {
        return commandService.validateCarData(carRequest);
    }

    @Override
    public PageResponse<CarResponse> searchCars(CarSearchRequest searchRequest) {
        return queryService.searchCars(searchRequest);
    }

    @Override
    public Page<CarResponse> getAllCars(Pageable pageable) {
        return queryService.getAllCars(pageable);
    }

    @Override
    public Page<CarResponse> getCarsByStatus(String status, Pageable pageable) {
        return queryService.getCarsByStatus(status, pageable);
    }

    @Override
    public long getCarCount() {
        return queryService.getCarCount();
    }

    @Override
    public Page<CarResponse> getCarsByBrand(String brand, Pageable pageable) {
        return queryService.getCarsByBrand(brand, pageable);
    }

    @Override
    public Page<CarResponse> getCarsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return queryService.getCarsByPriceRange(minPrice, maxPrice, pageable);
    }

    @Override
    public Page<CarResponse> getNewCars(Pageable pageable) {
        return queryService.getNewCars(pageable);
    }

    @Override
    public Page<CarResponse> getAllActiveCars(Pageable pageable) {
        return queryService.getAllActiveCars(pageable);
    }

    @Override
    public long getActiveCarCount() {
        return queryService.getActiveCarCount();
    }

    @Override
    public boolean existsByLicensePlate(String licensePlate) {
        return queryService.existsByLicensePlate(licensePlate);
    }

    @Override
    public Page<CarResponse> searchCarsByCriteria(String searchTerm, String brand, String model,
                                                  BigDecimal minPrice, BigDecimal maxPrice,
                                                  CarStatusType status, int page, int size) {
        return queryService.searchCarsByCriteria(searchTerm, brand, model, minPrice, maxPrice, status, page, size);
    }

    @Override
    public int countByStatus(CarStatusType status) {
        return queryService.countByStatus(status);
    }

    @Override
    public int countTotalActiveCars() {
        return queryService.countTotalActiveCars();
    }

    @Override
    public java.util.Map<Long, CarStatusType> findActiveCarStatuses() {
        return queryService.findActiveCarStatuses();
    }

    @Override
    public CarResponse markAsMaintenance(Long id) {
        return statusService.markAsMaintenance(id);
    }

    @Override
    public CarResponse markAsAvailable(Long id) {
        return statusService.markAsAvailable(id);
    }

    @Override
    public CarResponse updateCarStatus(Long id, CarStatusUpdateRequest statusUpdateRequest) {
        return statusService.updateCarStatus(id, statusUpdateRequest);
    }

    @Override
    public Map<String, Object> getCarStatistics() {
        return statisticsService.getCarStatistics();
    }

    @Override
    public Map<String, Long> getCarsCountByStatus() {
        return statisticsService.getCarsCountByStatus();
    }

    @Override
    public Map<String, Long> getCarsCountByBrand() {
        return statisticsService.getCarsCountByBrand();
    }

    @Override
    public Map<String, BigDecimal> getAveragePriceByBrand() {
        return statisticsService.getAveragePriceByBrand();
    }

    @Override
    public FilterOptionsResponse getFilterOptions() {
        return statisticsService.getFilterOptions();
    }
}
