package com.akif.service;

import com.akif.dto.request.RentalRequestDto;
import com.akif.dto.response.RentalResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IRentalService {

    RentalResponseDto requestRental(RentalRequestDto request, String username);

    RentalResponseDto confirmRental(Long rentalId);

    RentalResponseDto pickupRental(Long rentalId, String pickupNotes);

    RentalResponseDto returnRental(Long rentalId, String returnNotes);

    RentalResponseDto cancelRental(Long rentalId, String username);

    Page<RentalResponseDto> getMyRentals(String username, Pageable pageable);

    Page<RentalResponseDto> getAllRentals(Pageable pageable);

    RentalResponseDto getRentalById(Long id, String username);
}