package com.akif.rental.internal.service.report.impl;

import com.akif.rental.internal.dto.report.LateReturnFilterDto;
import com.akif.rental.internal.dto.report.LateReturnReportDto;
import com.akif.rental.internal.dto.report.LateReturnStatisticsDto;
import com.akif.rental.domain.enums.LateReturnStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.rental.internal.service.report.LateReturnReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LateReturnReportServiceImpl implements LateReturnReportService {

    private final RentalRepository rentalRepository;

    @Override
    public Page<LateReturnReportDto> getLateReturns(LateReturnFilterDto filter, Pageable pageable) {
        log.debug("Getting late returns with filter: {}", filter);

        List<LateReturnStatus> statuses = determineStatuses(filter);

        Pageable sortedPageable = applySorting(filter, pageable);

        Page<Rental> rentals = rentalRepository.findLateReturns(
                statuses,
                filter.startDate(),
                filter.endDate(),
                sortedPageable
        );

        log.info("Found {} late returns", rentals.getTotalElements());

        return rentals.map(this::mapToReportDto);
    }

    @Override
    public LateReturnStatisticsDto getStatistics(LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating late return statistics from {} to {}", startDate, endDate);

        List<LateReturnStatus> lateStatuses = Arrays.asList(
                LateReturnStatus.LATE,
                LateReturnStatus.SEVERELY_LATE
        );

        long totalLateReturns = rentalRepository.countLateReturns(
                lateStatuses,
                startDate,
                endDate
        );

        long severelyLateCount = rentalRepository.countSeverelyLateReturns(
                startDate,
                endDate
        );

        BigDecimal totalPenaltyAmount = rentalRepository.sumTotalPenaltyAmount(
                lateStatuses,
                startDate,
                endDate
        );

        BigDecimal collectedPenaltyAmount = rentalRepository.sumCollectedPenaltyAmount(
                lateStatuses,
                startDate,
                endDate
        );

        BigDecimal pendingPenaltyAmount = totalPenaltyAmount.subtract(collectedPenaltyAmount);

        Double averageLateHours = rentalRepository.averageLateHours(
                lateStatuses,
                startDate,
                endDate
        );

        long totalReturns = rentalRepository.countTotalReturns(startDate, endDate);
        double lateReturnPercentage = calculatePercentage(totalLateReturns, totalReturns);

        log.info("Statistics: {} late returns, {} severely late, total penalty: {}",
                totalLateReturns, severelyLateCount, totalPenaltyAmount);

        return new LateReturnStatisticsDto(
                (int) totalLateReturns,
                (int) severelyLateCount,
                totalPenaltyAmount,
                collectedPenaltyAmount,
                pendingPenaltyAmount,
                averageLateHours,
                lateReturnPercentage
        );
    }

    private List<LateReturnStatus> determineStatuses(LateReturnFilterDto filter) {
        if (filter.status() != null) {
            return List.of(filter.status());
        }
        return Arrays.asList(LateReturnStatus.LATE, LateReturnStatus.SEVERELY_LATE);
    }

    private Pageable applySorting(LateReturnFilterDto filter, Pageable pageable) {
        String sortBy = filter.sortBy() != null ? filter.sortBy() : "endDate";
        String sortDirection = filter.sortDirection() != null ? filter.sortDirection() : "DESC";

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Sort sort = Sort.by(direction, sortBy).and(Sort.by(direction, "id"));

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );
    }

    private LateReturnReportDto mapToReportDto(Rental rental) {
        Integer lateDays = rental.getLateHours() != null
                ? (rental.getLateHours() + 23) / 24
                : null;

        return new LateReturnReportDto(
                rental.getId(),
                rental.getUserEmail(),
                rental.getUserEmail(),
                rental.getCarBrand(),
                rental.getCarModel(),
                rental.getCarLicensePlate(),
                rental.getEndDate(),
                rental.getActualReturnTime(),
                rental.getLateHours(),
                lateDays,
                rental.getLateReturnStatus(),
                rental.getPenaltyAmount(),
                rental.getCurrency(),
                rental.getPenaltyPaid()
        );
    }

    private double calculatePercentage(long part, long total) {
        if (total == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
