package com.akif.car.internal.service.availability;

import com.akif.car.internal.dto.availability.SimilarCarDto;

import java.time.LocalDate;
import java.util.List;

public interface SimilarCarService {

    List<SimilarCarDto> findSimilarAvailableCars(Long carId, LocalDate startDate, LocalDate endDate, Integer limit);
}
