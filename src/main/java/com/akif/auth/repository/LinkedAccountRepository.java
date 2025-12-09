package com.akif.auth.repository;

import com.akif.shared.enums.OAuth2Provider;
import com.akif.auth.domain.LinkedAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LinkedAccountRepository extends JpaRepository<LinkedAccount, Long> {

    Optional<LinkedAccount> findByProviderAndProviderId(OAuth2Provider provider, String providerId);

    Optional<LinkedAccount> findByUserIdAndProvider(Long userId, OAuth2Provider provider);

    boolean existsByProviderAndProviderId(OAuth2Provider provider, String providerId);
}
