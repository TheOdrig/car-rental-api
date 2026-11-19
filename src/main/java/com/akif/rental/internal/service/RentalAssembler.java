package com.akif.rental.internal.service;

import com.akif.payment.api.PaymentService;
import com.akif.payment.api.PaymentStatus;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.mapper.RentalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RentalAssembler {

    private final RentalMapper rentalMapper;
    private final PaymentService paymentService;

    public RentalResponse toResponse(Rental rental) {
        return toResponses(List.of(rental)).get(0);
    }

    public RentalResponse toResponse(Rental rental, PaymentStatus paymentStatus) {
        return rentalMapper.toDto(rental).toBuilder().paymentStatus(paymentStatus).build();
    }

    public Page<RentalResponse> toPage(Page<Rental> rentals) {
        return new PageImpl<>(toResponses(rentals.getContent()), rentals.getPageable(), rentals.getTotalElements());
    }

    public List<RentalResponse> toResponses(Collection<Rental> rentals) {
        if (rentals.isEmpty()) {
            return List.of();
        }
        Map<Long, PaymentStatus> statuses = paymentService.getRentalPaymentStatuses(
                rentals.stream().map(Rental::getId).toList());
        return rentals.stream()
                .map(rental -> rentalMapper.toDto(rental).toBuilder()
                        .paymentStatus(statuses.get(rental.getId()))
                        .build())
                .toList();
    }
}
