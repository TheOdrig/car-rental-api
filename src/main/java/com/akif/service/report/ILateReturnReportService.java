package com.akif.service.report;

import com.akif.dto.report.LateReturnFilterDto;
import com.akif.dto.report.LateReturnReportDto;
import com.akif.dto.report.LateReturnStatisticsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ILateReturnReportService {

    Page<LateReturnReportDto> getLateReturns(LateReturnFilterDto filter, Pageable pageable);

    LateReturnStatisticsDto getStatistics(LocalDate startDate, LocalDate endDate);
}
