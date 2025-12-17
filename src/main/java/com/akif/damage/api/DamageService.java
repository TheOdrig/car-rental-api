package com.akif.damage.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DamageService {

    List<DamageReportDto> getDamageReportsByRentalId(Long rentalId);

    boolean hasPendingDamageReports(Long rentalId);

    boolean hasPendingDamageReportsForCar(Long carId);


    int countPendingAssessments();

    int countUnresolvedDisputesOlderThan(int days);

    BigDecimal sumDamageCharges(LocalDate startDate, LocalDate endDate);
}