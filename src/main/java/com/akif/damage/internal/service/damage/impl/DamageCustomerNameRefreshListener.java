package com.akif.damage.internal.service.damage.impl;

import com.akif.auth.api.UserProfileUpdatedEvent;
import com.akif.damage.internal.repository.DamageReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class DamageCustomerNameRefreshListener {

    private final DamageReportRepository damageReportRepository;

    @EventListener
    public void onProfileUpdated(UserProfileUpdatedEvent event) {
        damageReportRepository.refreshCustomerName(event.userId(), event.displayName());
    }
}
