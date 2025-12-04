package com.akif.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PenaltyWaiverResponseDto implements Serializable {

    private Long id;
    private Long rentalId;
    private BigDecimal originalPenalty;
    private BigDecimal waivedAmount;
    private BigDecimal remainingPenalty;
    private String reason;
    private Long adminId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime waivedAt;

    private Boolean refundInitiated;
    private String refundTransactionId;
}
