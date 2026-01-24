package com.akif.auth.internal.repository;

import com.akif.auth.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByUsernameAndIsDeletedFalse(String username);

    @Query(value = """
                SELECT DISTINCT u.* FROM gallery.users u
                LEFT JOIN gallery.user_roles r ON u.id = r.user_id
                WHERE u.is_deleted = false
                AND (CAST(:role AS TEXT) IS NULL OR r.role = CAST(:role AS TEXT))
                AND (CAST(:status AS TEXT) IS NULL OR
                    (CAST(:status AS TEXT) = 'ACTIVE' AND u.is_banned = false AND u.enabled = true) OR
                    (CAST(:status AS TEXT) = 'BANNED' AND u.is_banned = true) OR
                    (CAST(:status AS TEXT) = 'PENDING' AND u.enabled = false AND u.is_banned = false))
                AND (CAST(:search AS TEXT) IS NULL OR LOWER(u.email) LIKE LOWER('%' || CAST(:search AS TEXT) || '%')
                    OR LOWER(u.first_name) LIKE LOWER('%' || CAST(:search AS TEXT) || '%')
                    OR LOWER(u.last_name) LIKE LOWER('%' || CAST(:search AS TEXT) || '%'))
                ORDER BY u.create_time DESC
            """, countQuery = """
                SELECT COUNT(DISTINCT u.id) FROM gallery.users u
                LEFT JOIN gallery.user_roles r ON u.id = r.user_id
                WHERE u.is_deleted = false
                AND (CAST(:role AS TEXT) IS NULL OR r.role = CAST(:role AS TEXT))
                AND (CAST(:status AS TEXT) IS NULL OR
                    (CAST(:status AS TEXT) = 'ACTIVE' AND u.is_banned = false AND u.enabled = true) OR
                    (CAST(:status AS TEXT) = 'BANNED' AND u.is_banned = true) OR
                    (CAST(:status AS TEXT) = 'PENDING' AND u.enabled = false AND u.is_banned = false))
                AND (CAST(:search AS TEXT) IS NULL OR LOWER(u.email) LIKE LOWER('%' || CAST(:search AS TEXT) || '%')
                    OR LOWER(u.first_name) LIKE LOWER('%' || CAST(:search AS TEXT) || '%')
                    OR LOWER(u.last_name) LIKE LOWER('%' || CAST(:search AS TEXT) || '%'))
            """, nativeQuery = true)
    Page<User> findAllForAdmin(
            @Param("role") String role,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);

    long countByIsDeletedFalse();

    long countByIsDeletedFalseAndEnabledTrueAndIsBannedFalse();

    long countByIsDeletedFalseAndIsBannedTrue();
}