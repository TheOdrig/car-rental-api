package com.akif.service.damage.impl;

import com.akif.dto.damage.request.DamageSearchFilterDto;
import com.akif.dto.damage.response.DamageReportDto;
import com.akif.dto.damage.response.DamageStatisticsDto;
import com.akif.enums.DamageSeverity;
import com.akif.enums.DamageStatus;
import com.akif.model.DamageReport;
import com.akif.repository.DamageReportRepository;
import com.akif.service.damage.IDamageHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DamageHistoryServiceImpl implements IDamageHistoryService {

    private final DamageReportRepository damageReportRepository;

    @Override
    public Page<DamageReportDto> getDamagesByVehicle(Long carId, Pageable pageable) {
        return damageReportRepository.findByCarIdAndIsDeletedFalse(carId, pageable)
                .map(this::mapToDto);
    }

    @Override
    public Page<DamageReportDto> getDamagesByCustomer(Long userId, Pageable pageable) {
        return damageReportRepository.findByRental_UserIdAndIsDeletedFalse(userId, pageable)
                .map(this::mapToDto);
    }

    @Override
    public Page<DamageReportDto> searchDamages(DamageSearchFilterDto filter, Pageable pageable) {
        return damageReportRepository.searchDamages(
                filter.startDate(),
                filter.endDate(),
                filter.severity(),
                filter.category(),
                filter.status(),
                filter.carId(),
                filter.customerId(),
                pageable
        ).map(this::mapToDto);
    }

    @Override
    public DamageStatisticsDto getDamageStatistics(LocalDate startDate, LocalDate endDate) {
        int totalDamages = damageReportRepository.countTotalDamages(startDate, endDate);
        int minorCount = damageReportRepository.countBySeverity(DamageSeverity.MINOR, startDate, endDate);
        int moderateCount = damageReportRepository.countBySeverity(DamageSeverity.MODERATE, startDate, endDate);
        int majorCount = damageReportRepository.countBySeverity(DamageSeverity.MAJOR, startDate, endDate);
        int totalLossCount = damageReportRepository.countBySeverity(DamageSeverity.TOTAL_LOSS, startDate, endDate);
        int disputedCount = damageReportRepository.countByStatus(DamageStatus.DISPUTED, startDate, endDate);
        int resolvedCount = damageReportRepository.countByStatus(DamageStatus.RESOLVED, startDate, endDate);

        return new DamageStatisticsDto(
                totalDamages,
                minorCount,
                moderateCount,
                majorCount,
                totalLossCount,
                damageReportRepository.sumTotalRepairCost(startDate, endDate),
                damageReportRepository.sumTotalCustomerLiability(startDate, endDate),
                damageReportRepository.averageRepairCost(startDate, endDate),
                disputedCount,
                resolvedCount
        );
    }

    private DamageReportDto mapToDto(DamageReport damageReport) {
        return new DamageReportDto(
                damageReport.getId(),
                damageReport.getRental().getId(),
                damageReport.getCar().getId(),
                damageReport.getCar().getLicensePlate(),
                damageReport.getRental().getUser().getUsername(),
                damageReport.getDescription(),
                damageReport.getSeverity(),
                damageReport.getCategory(),
                damageReport.getStatus(),
                damageReport.getRepairCostEstimate(),
                damageReport.getCustomerLiability(),
                damageReport.getReportedAt()
        );
    }
}
