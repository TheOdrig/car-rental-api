package com.akif.rental.internal.listener;

import com.akif.auth.api.UserProfileUpdatedEvent;
import com.akif.rental.internal.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerNameRefreshListener {

    private final RentalRepository rentalRepository;

    @EventListener
    public void onProfileUpdated(UserProfileUpdatedEvent event) {
        rentalRepository.refreshCustomerName(event.userId(), event.displayName());
    }
}
