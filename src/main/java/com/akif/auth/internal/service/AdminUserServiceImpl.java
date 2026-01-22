package com.akif.auth.internal.service;

import com.akif.auth.api.AdminUserDetailResponse;
import com.akif.auth.api.AdminUserDetailResponse.AccountStatus;
import com.akif.auth.api.AdminUserDetailResponse.UserStatistics;
import com.akif.auth.api.AdminUserDetailResponse.VerificationInfo;
import com.akif.auth.api.AdminUserService;
import com.akif.auth.domain.User;
import com.akif.auth.internal.exception.UserNotFoundException;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.damage.api.DamageService;
import com.akif.damage.api.UserDamageStatisticsDto;
import com.akif.rental.api.RentalService;
import com.akif.rental.api.UserRentalStatisticsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RentalService rentalService;
    private final DamageService damageService;

    @Override
    public AdminUserDetailResponse getUserDetailForAdmin(Long userId) {
        log.debug("Fetching user detail for admin: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        VerificationInfo verification = buildVerificationInfo(user);
        UserStatistics statistics = calculateUserStatistics(userId, user);
        AccountStatus accountStatus = buildAccountStatus(user);

        return new AdminUserDetailResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getRoles(),
                user.getCreateTime(),
                null,
                verification,
                statistics,
                accountStatus);
    }

    private VerificationInfo buildVerificationInfo(User user) {
        return new VerificationInfo(
                true,
                user.getPhone() != null && !user.getPhone().isEmpty(),
                "PENDING",
                Collections.emptyList());
    }

    private UserStatistics calculateUserStatistics(Long userId, User user) {
        UserRentalStatisticsDto rentalStats = rentalService.getUserStatistics(userId);
        UserDamageStatisticsDto damageStats = damageService.getUserDamageStatistics(userId);

        int customerSinceDays = 0;
        if (user.getCreateTime() != null) {
            customerSinceDays = (int) ChronoUnit.DAYS.between(
                    user.getCreateTime().toLocalDate(),
                    LocalDateTime.now().toLocalDate());
        }

        return new UserStatistics(
                rentalStats.totalRentals(),
                rentalStats.completedRentals(),
                rentalStats.cancelledRentals(),
                rentalStats.activeRentals(),
                rentalStats.totalSpent(),
                rentalStats.averageRentalDuration(),
                damageStats.totalDamageReports(),
                damageStats.totalDamageCost(),
                rentalStats.lateReturns(),
                customerSinceDays);
    }

    private AccountStatus buildAccountStatus(User user) {
        String status = user.getEnabled() != null && user.getEnabled() ? "ACTIVE" : "INACTIVE";
        if (user.getIsDeleted() != null && user.getIsDeleted()) {
            status = "DELETED";
        }

        return new AccountStatus(
                status,
                null,
                null,
                null,
                user.getUpdateTime());
    }
}
