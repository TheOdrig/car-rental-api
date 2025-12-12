package com.akif.payment.internal.mapper;

import com.akif.payment.api.PaymentDto;
import com.akif.payment.domain.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {
    
    PaymentDto toDto(Payment payment);
    
    List<PaymentDto> toDtoList(List<Payment> payments);
}
