package com.akif.damage.internal.service.damage.impl;

import com.akif.damage.api.DamageReportDto;
import com.akif.damage.api.DamageSeverity;
import com.akif.damage.api.DamageStatus;
import com.akif.damage.internal.service.damage.DamageHistoryService;
import com.akif.damage.internal.dto.damage.request.DamageSearchFilterDto;
import com.akif.damage.internal.dto.damage.response.DamageCurrencyTotals;
import com.akif.damage.internal.dto.damage.response.DamageStatisticsDto;
import com.akif.shared.enums.CurrencyType;
import com.akif.damage.internal.mapper.DamageMapper;
import com.akif.damage.internal.repository.DamageReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DamageHistoryServiceImpl implements DamageHistoryService {

    private final DamageReportRepository damageReportRepository;
    private final DamageMapper damageMapper;

    @Override
    public Page<DamageReportDto> getDamagesByVehicle(Long carId, Pageable pageable) {
        return damageReportRepository.findByCarIdAndIsDeletedFalse(carId, pageable)
                .map(damageMapper::toPublicDto);
    }

    @Override
    public Page<DamageReportDto> getDamagesByCustomer(Long userId, Pageable pageable) {
        return damageReportRepository.findByCustomerUserIdAndIsDeletedFalse(userId, pageable)
                .map(damageMapper::toPublicDto);
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
                pageable
        ).map(damageMapper::toPublicDto);
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
                totalsByCurrency(startDate, endDate),
                disputedCount,
                resolvedCount
        );
    }

    private List<DamageCurrencyTotals> totalsByCurrency(LocalDate startDate, LocalDate endDate) {
        return damageReportRepository.totalsByCurrency(startDate, endDate).stream()
                .map(row -> {
                    BigDecimal repairCost = (BigDecimal) row[2];
                    long estimated = (Long) row[4];
                    BigDecimal average = estimated == 0
                            ? BigDecimal.ZERO.setScale(2)
                            : repairCost.divide(BigDecimal.valueOf(estimated), 2, RoundingMode.HALF_UP);
                    return new DamageCurrencyTotals((CurrencyType) row[0], (Long) row[1], repairCost,
                            (BigDecimal) row[3], average);
                })
                .toList();
    }
}
