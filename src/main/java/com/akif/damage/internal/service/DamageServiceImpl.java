package com.akif.damage.internal.service;

import com.akif.damage.api.DamageService;
import com.akif.damage.api.DamageReportDto;
import com.akif.damage.domain.model.DamageReport;
import com.akif.damage.internal.mapper.DamageMapper;
import com.akif.damage.internal.repository.DamageReportRepository;
import com.akif.damage.internal.exception.DamageReportException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DamageServiceImpl implements DamageService {

    private final DamageReportRepository damageReportRepository;
    private final DamageMapper damageMapper;

    @Override
    public List<DamageReportDto> getDamageReportsByRentalId(Long rentalId) {
        log.debug("Getting damage reports for rental: {}", rentalId);
        
        List<DamageReport> damageReports = damageReportRepository.findByRentalIdAndIsDeletedFalse(rentalId);
        
        return damageMapper.toPublicDtoList(damageReports);
    }

    @Override
    public boolean hasPendingDamageReports(Long rentalId) {
        log.debug("Checking pending damage reports for rental: {}", rentalId);
        
        return damageReportRepository.existsPendingByRentalId(rentalId);
    }

    @Override
    public boolean hasPendingDamageReportsForCar(Long carId) {
        log.debug("Checking pending damage reports for car: {}", carId);
        
        return damageReportRepository.existsPendingByCarId(carId);
    }


    @Override
    public int countPendingAssessments() {
        return damageReportRepository.countPendingAssessments();
    }

    @Override
    public int countUnresolvedDisputesOlderThan(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return damageReportRepository.countUnresolvedDisputesOlderThan(cutoffDate);
    }

    @Override
    public BigDecimal sumDamageCharges(LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating damage charges between {} and {}", startDate, endDate);
        return damageReportRepository.sumTotalCustomerLiability(startDate, endDate);
    }
}
