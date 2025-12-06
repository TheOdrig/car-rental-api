package com.akif.service.damage;

import com.akif.dto.damage.request.DamageSearchFilterDto;
import com.akif.dto.damage.response.DamageReportDto;
import com.akif.dto.damage.response.DamageStatisticsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface IDamageHistoryService {

    Page<DamageReportDto> getDamagesByVehicle(Long carId, Pageable pageable);

    Page<DamageReportDto> getDamagesByCustomer(Long userId, Pageable pageable);

    Page<DamageReportDto> searchDamages(DamageSearchFilterDto filter, Pageable pageable);

    DamageStatisticsDto getDamageStatistics(LocalDate startDate, LocalDate endDate);
}
