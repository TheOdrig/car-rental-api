package com.akif.damage.internal.service.damage;

import com.akif.damage.internal.dto.damage.request.DamageSearchFilterDto;
import com.akif.damage.api.DamageReportDto;
import com.akif.damage.internal.dto.damage.response.DamageStatisticsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface DamageHistoryService {

    Page<DamageReportDto> getDamagesByVehicle(Long carId, Pageable pageable);

    Page<DamageReportDto> getDamagesByCustomer(Long userId, Pageable pageable);

    DamageReportDto getCustomerDamage(Long damageId, Long userId);

    Page<DamageReportDto> searchDamages(DamageSearchFilterDto filter, Pageable pageable);

    DamageStatisticsDto getDamageStatistics(LocalDate startDate, LocalDate endDate);
}
