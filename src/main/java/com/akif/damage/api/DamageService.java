package com.akif.damage.api;

import com.akif.auth.api.UserDamageStatisticsProvider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DamageService extends UserDamageStatisticsProvider {

    List<DamageReportDto> getDamageReportsByRentalId(Long rentalId);

    boolean hasPendingDamageReports(Long rentalId);

    boolean hasPendingDamageReportsForCar(Long carId);

    int countPendingAssessments();

    int countUnresolvedDisputesOlderThan(int days);

    List<RepairCostTotal> sumRepairCostsByCurrency(LocalDate startDate, LocalDate endDate);

}
