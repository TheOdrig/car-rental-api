package com.akif.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PenaltyWaiverRequestDto implements Serializable {

    @DecimalMin(value = "0.01", message = "Waiver amount must be greater than 0")
    private BigDecimal waiverAmount;

    @NotBlank(message = "Reason cannot be blank")
    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
    private String reason;

    private Boolean fullWaiver;
}
