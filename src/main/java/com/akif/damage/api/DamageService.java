package com.akif.damage.api;

import java.util.List;

public interface DamageService {

    List<DamageReportDto> getDamageReportsByRentalId(Long rentalId);

    boolean hasPendingDamageReports(Long rentalId);

    boolean hasPendingDamageReportsForCar(Long carId);
}
