package com.akif.model;

import com.akif.shared.enums.OAuth2Provider;
import com.akif.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Entity
@Table(name = "linked_accounts",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_linked_account_provider_id",
                columnNames = {"provider", "provider_id"}))
@Getter
@Setter
@ToString(exclude = "user")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LinkedAccount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "provider", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OAuth2Provider provider;

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy 
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() 
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy 
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() 
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        LinkedAccount that = (LinkedAccount) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy 
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() 
                : getClass().hashCode();
    }
}
