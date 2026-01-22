package com.akif.auth.unit;

import com.akif.auth.api.AdminUserDetailResponse;
import com.akif.auth.api.AdminUserDetailResponse.UserStatistics;
import com.akif.auth.domain.User;
import com.akif.auth.internal.exception.UserNotFoundException;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.auth.internal.service.AdminUserServiceImpl;
import com.akif.damage.api.DamageService;
import com.akif.damage.api.UserDamageStatisticsDto;
import com.akif.rental.api.RentalService;
import com.akif.rental.api.UserRentalStatisticsDto;
import com.akif.shared.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserService Unit Tests")
class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RentalService rentalService;

    @Mock
    private DamageService damageService;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User testUser;
    private UserRentalStatisticsDto testRentalStats;
    private UserDamageStatisticsDto testDamageStats;

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
        testRentalStats = createRentalStatistics();
        testDamageStats = createDamageStatistics();
    }

    @Nested
    @DisplayName("getUserDetailForAdmin Tests")
    class GetUserDetailForAdminTests {

        @Test
        @DisplayName("Should return complete user details with statistics for valid user")
        void shouldReturnCompleteUserDetailsWithStatistics() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(rentalService.getUserStatistics(1L)).thenReturn(testRentalStats);
            when(damageService.getUserDamageStatistics(1L)).thenReturn(testDamageStats);

            AdminUserDetailResponse result = adminUserService.getUserDetailForAdmin(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.firstName()).isEqualTo("John");
            assertThat(result.lastName()).isEqualTo("Doe");
            assertThat(result.email()).isEqualTo("john@example.com");
            assertThat(result.phone()).isEqualTo("+1234567890");
            assertThat(result.roles()).containsExactly(Role.USER);

            verify(userRepository).findById(1L);
            verify(rentalService).getUserStatistics(1L);
            verify(damageService).getUserDamageStatistics(1L);
        }

        @Test
        @DisplayName("Should include verification info in response")
        void shouldIncludeVerificationInfo() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(rentalService.getUserStatistics(1L)).thenReturn(testRentalStats);
            when(damageService.getUserDamageStatistics(1L)).thenReturn(testDamageStats);

            AdminUserDetailResponse result = adminUserService.getUserDetailForAdmin(1L);

            assertThat(result.verification()).isNotNull();
            assertThat(result.verification().emailVerified()).isTrue();
            assertThat(result.verification().phoneVerified()).isTrue();
        }

        @Test
        @DisplayName("Should include correct statistics in response")
        void shouldIncludeCorrectStatistics() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(rentalService.getUserStatistics(1L)).thenReturn(testRentalStats);
            when(damageService.getUserDamageStatistics(1L)).thenReturn(testDamageStats);

            AdminUserDetailResponse result = adminUserService.getUserDetailForAdmin(1L);

            UserStatistics stats = result.statistics();
            assertThat(stats).isNotNull();
            assertThat(stats.totalRentals()).isEqualTo(15);
            assertThat(stats.completedRentals()).isEqualTo(12);
            assertThat(stats.cancelledRentals()).isEqualTo(2);
            assertThat(stats.activeRentals()).isEqualTo(1);
            assertThat(stats.totalSpent()).isEqualByComparingTo(new BigDecimal("4500.00"));
            assertThat(stats.averageRentalDuration()).isEqualTo(5.2);
            assertThat(stats.lateReturns()).isEqualTo(2);
            assertThat(stats.totalDamageReports()).isEqualTo(3);
            assertThat(stats.totalDamageCost()).isEqualByComparingTo(new BigDecimal("350.00"));
        }

        @Test
        @DisplayName("Should include account status in response")
        void shouldIncludeAccountStatus() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(rentalService.getUserStatistics(1L)).thenReturn(testRentalStats);
            when(damageService.getUserDamageStatistics(1L)).thenReturn(testDamageStats);

            AdminUserDetailResponse result = adminUserService.getUserDetailForAdmin(1L);

            assertThat(result.accountStatus()).isNotNull();
            assertThat(result.accountStatus().status()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user does not exist")
        void shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminUserService.getUserDetailForAdmin(999L))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("999");

            verify(userRepository).findById(999L);
            verifyNoInteractions(rentalService);
            verifyNoInteractions(damageService);
        }
    }

    @Nested
    @DisplayName("Statistics Edge Cases")
    class StatisticsEdgeCases {

        @Test
        @DisplayName("Should handle user with zero rentals")
        void shouldHandleUserWithZeroRentals() {
            UserRentalStatisticsDto zeroRentalStats = new UserRentalStatisticsDto(0, 0, 0, 0, BigDecimal.ZERO, 0.0, 0);
            UserDamageStatisticsDto zeroDamageStats = new UserDamageStatisticsDto(0, BigDecimal.ZERO);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(rentalService.getUserStatistics(1L)).thenReturn(zeroRentalStats);
            when(damageService.getUserDamageStatistics(1L)).thenReturn(zeroDamageStats);

            AdminUserDetailResponse result = adminUserService.getUserDetailForAdmin(1L);

            assertThat(result.statistics().totalRentals()).isZero();
            assertThat(result.statistics().completedRentals()).isZero();
            assertThat(result.statistics().totalSpent()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.statistics().totalDamageReports()).isZero();
            assertThat(result.statistics().totalDamageCost()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should handle user with zero damages")
        void shouldHandleUserWithZeroDamages() {
            UserDamageStatisticsDto zeroDamageStats = new UserDamageStatisticsDto(0, BigDecimal.ZERO);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(rentalService.getUserStatistics(1L)).thenReturn(testRentalStats);
            when(damageService.getUserDamageStatistics(1L)).thenReturn(zeroDamageStats);

            AdminUserDetailResponse result = adminUserService.getUserDetailForAdmin(1L);

            assertThat(result.statistics().totalDamageReports()).isZero();
            assertThat(result.statistics().totalDamageCost()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should handle user without phone number")
        void shouldHandleUserWithoutPhoneNumber() {
            User userWithoutPhone = User.builder()
                    .id(2L)
                    .username("nophone")
                    .email("nophone@example.com")
                    .firstName("No")
                    .lastName("Phone")
                    .phone(null)
                    .roles(Set.of(Role.USER))
                    .enabled(true)
                    .isDeleted(false)
                    .createTime(LocalDateTime.now().minusDays(30))
                    .updateTime(LocalDateTime.now())
                    .build();

            when(userRepository.findById(2L)).thenReturn(Optional.of(userWithoutPhone));
            when(rentalService.getUserStatistics(2L)).thenReturn(testRentalStats);
            when(damageService.getUserDamageStatistics(2L)).thenReturn(testDamageStats);

            AdminUserDetailResponse result = adminUserService.getUserDetailForAdmin(2L);

            assertThat(result.phone()).isNull();
            assertThat(result.verification().phoneVerified()).isFalse();
        }

        @Test
        @DisplayName("Should handle disabled user")
        void shouldHandleDisabledUser() {
            User disabledUser = User.builder()
                    .id(3L)
                    .username("disabled")
                    .email("disabled@example.com")
                    .firstName("Dis")
                    .lastName("Abled")
                    .roles(Set.of(Role.USER))
                    .enabled(false)
                    .isDeleted(false)
                    .createTime(LocalDateTime.now().minusDays(30))
                    .updateTime(LocalDateTime.now())
                    .build();

            when(userRepository.findById(3L)).thenReturn(Optional.of(disabledUser));
            when(rentalService.getUserStatistics(3L)).thenReturn(testRentalStats);
            when(damageService.getUserDamageStatistics(3L)).thenReturn(testDamageStats);

            AdminUserDetailResponse result = adminUserService.getUserDetailForAdmin(3L);

            assertThat(result.accountStatus().status()).isEqualTo("INACTIVE");
        }

        @Test
        @DisplayName("Should handle deleted user")
        void shouldHandleDeletedUser() {
            User deletedUser = User.builder()
                    .id(4L)
                    .username("deleted")
                    .email("deleted@example.com")
                    .firstName("Del")
                    .lastName("Eted")
                    .roles(Set.of(Role.USER))
                    .enabled(true)
                    .isDeleted(true)
                    .createTime(LocalDateTime.now().minusDays(30))
                    .updateTime(LocalDateTime.now())
                    .build();

            when(userRepository.findById(4L)).thenReturn(Optional.of(deletedUser));
            when(rentalService.getUserStatistics(4L)).thenReturn(testRentalStats);
            when(damageService.getUserDamageStatistics(4L)).thenReturn(testDamageStats);

            AdminUserDetailResponse result = adminUserService.getUserDetailForAdmin(4L);

            assertThat(result.accountStatus().status()).isEqualTo("DELETED");
        }
    }

    private User createTestUser() {
        return User.builder()
                .id(1L)
                .username("johndoe")
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .phone("+1234567890")
                .avatarUrl("https://example.com/avatar.jpg")
                .roles(Set.of(Role.USER))
                .enabled(true)
                .isDeleted(false)
                .createTime(LocalDateTime.now().minusDays(180))
                .updateTime(LocalDateTime.now())
                .build();
    }

    private UserRentalStatisticsDto createRentalStatistics() {
        return new UserRentalStatisticsDto(15, 12, 2, 1, new BigDecimal("4500.00"), 5.2, 2);
    }

    private UserDamageStatisticsDto createDamageStatistics() {
        return new UserDamageStatisticsDto(3, new BigDecimal("350.00"));
    }
}
