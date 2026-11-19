package com.akif.damage.internal.service.damage.impl;

import com.akif.damage.internal.repository.DamagePhotoRepository;
import com.akif.shared.infrastructure.FileAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class DamagePhotoAccessPolicy implements FileAccessPolicy {

    private final DamagePhotoRepository damagePhotoRepository;

    @Override
    public String directory() {
        return "damage-photos";
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOwnedBy(String filename, Long userId) {
        return damagePhotoRepository.findCustomerIdsByFileName(filename).contains(userId);
    }
}
