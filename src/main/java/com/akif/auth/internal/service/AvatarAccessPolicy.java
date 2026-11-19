package com.akif.auth.internal.service;

import com.akif.auth.internal.repository.UserRepository;
import com.akif.shared.infrastructure.FileAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class AvatarAccessPolicy implements FileAccessPolicy {

    private final UserRepository userRepository;

    @Override
    public String directory() {
        return "avatars";
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOwnedBy(String filename, Long userId) {
        return userRepository.findIdsByAvatarFileName(filename).contains(userId);
    }
}
