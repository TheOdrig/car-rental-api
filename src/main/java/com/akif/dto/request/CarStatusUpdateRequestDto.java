package com.akif.dto.request;

import com.akif.enums.CarStatusType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarStatusUpdateRequestDto {

    @NotNull(message = "Car status type cannot be null")
    private CarStatusType carStatusType;

    private String reason;
    private String notes;
}
