package com.akif.dto.request;

import com.akif.enums.CarStatusType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarStatusUpdateRequestDto {

    private CarStatusType carStatusType;
    private String reason;
    private String notes;
}
