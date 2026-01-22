package com.akif.auth.internal.service;

import com.akif.auth.api.AdminNoteDto;
import com.akif.auth.api.AdminUserDetailResponse;
import com.akif.auth.api.AdminUserDetailResponse.AccountStatus;
import com.akif.auth.api.AdminUserDetailResponse.UserStatistics;
import com.akif.auth.api.AdminUserDetailResponse.VerificationInfo;
import com.akif.auth.api.AdminUserService;
import com.akif.auth.domain.AdminNote;
import com.akif.auth.domain.User;
import com.akif.auth.internal.exception.UserNotFoundException;
import com.akif.auth.internal.repository.AdminNoteRepository;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final AdminNoteRepository adminNoteRepository;
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
        String status;
        if (user.getIsBanned() != null && user.getIsBanned()) {
            status = "BANNED";
        } else if (user.getIsDeleted() != null && user.getIsDeleted()) {
            status = "DELETED";
        } else if (user.getEnabled() != null && user.getEnabled()) {
            status = "ACTIVE";
        } else {
            status = "INACTIVE";
        }

        return new AccountStatus(
                status,
                user.getBannedAt(),
                user.getBanReason(),
                user.getBannedBy(),
                user.getUpdateTime());
    }

    @Override
    @Transactional
    public void banUser(Long userId, String reason, Long adminId) {
        log.info("Banning user: userId={}, adminId={}", userId, adminId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.getIsBanned() != null && user.getIsBanned()) {
            log.warn("User {} is already banned", userId);
            throw new IllegalStateException("User is already banned");
        }

        user.setIsBanned(true);
        user.setBannedAt(LocalDateTime.now());
        user.setBannedBy(adminId);
        user.setBanReason(reason);
        user.setEnabled(false);

        userRepository.save(user);
        log.info("User {} has been banned by admin {}", userId, adminId);
    }

    @Override
    @Transactional
    public void unbanUser(Long userId, String note, Long adminId) {
        log.info("Unbanning user: userId={}, adminId={}", userId, adminId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.getIsBanned() == null || !user.getIsBanned()) {
            log.warn("User {} is not banned", userId);
            throw new IllegalStateException("User is not banned");
        }

        user.setIsBanned(false);
        user.setUnbannedAt(LocalDateTime.now());
        user.setEnabled(true);

        userRepository.save(user);
        log.info("User {} has been unbanned by admin {}", userId, adminId);
    }

    @Override
    @Transactional
    public AdminNoteDto addAdminNote(Long userId, String text, Long adminId, String adminUsername) {
        log.info("Adding admin note for user: userId={}, adminId={}", userId, adminId);

        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        AdminNote note = AdminNote.builder()
                .userId(userId)
                .adminId(adminId)
                .adminUsername(adminUsername)
                .text(text)
                .createdAt(LocalDateTime.now())
                .build();

        AdminNote saved = adminNoteRepository.save(note);
        log.info("Admin note {} created for user {}", saved.getId(), userId);

        return mapToDto(saved);
    }

    @Override
    public List<AdminNoteDto> getAdminNotes(Long userId) {
        log.debug("Getting admin notes for user: {}", userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<AdminNote> notes = adminNoteRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);

        return notes.stream()
                .map(this::mapToDto)
                .toList();
    }

    private AdminNoteDto mapToDto(AdminNote note) {
        return new AdminNoteDto(
                note.getId(),
                note.getUserId(),
                note.getAdminId(),
                note.getAdminUsername(),
                note.getText(),
                note.getCreatedAt());
    }
}
