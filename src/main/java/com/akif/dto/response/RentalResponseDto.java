package com.akif.dto.response;

import com.akif.enums.CurrencyType;
import com.akif.enums.RentalStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RentalResponseDto implements Serializable {

    private Long id;

    private CarSummaryResponseDto carSummary;
    private UserSummaryResponseDto userSummary;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate startDate;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate endDate;

    private Integer days;
    private BigDecimal dailyPrice;
    private BigDecimal totalPrice;
    private CurrencyType currency;
    private RentalStatus status;

    private BigDecimal convertedTotalPrice;
    private CurrencyType displayCurrency;
    private BigDecimal exchangeRate;
    private String rateSource;

    private String pickupNotes;
    private String returnNotes;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime updateTime;
}
