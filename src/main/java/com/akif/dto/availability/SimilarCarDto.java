package com.akif.dto.availability;

import com.akif.shared.enums.CurrencyType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimilarCarDto implements Serializable {

    private Long id;
    private String brand;
    private String model;
    private Integer productionYear;
    private String bodyType;
    private BigDecimal dailyRate;
    private BigDecimal totalPrice;
    private CurrencyType currency;
    private String imageUrl;
    private List<String> similarityReasons;
    private Integer similarityScore;
}
