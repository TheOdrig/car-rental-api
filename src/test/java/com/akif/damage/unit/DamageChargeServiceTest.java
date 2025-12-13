package com.akif.damage.unit;

import com.akif.damage.api.DamageChargedEvent;
import com.akif.damage.domain.enums.DamageCategory;
import com.akif.damage.domain.enums.DamageSeverity;
import com.akif.damage.domain.enums.DamageStatus;
import com.akif.damage.domain.model.DamageReport;
import com.akif.damage.internal.exception.DamageAssessmentException;
import com.akif.damage.internal.repository.DamageReportRepository;
import com.akif.damage.internal.service.damage.impl.DamageChargeServiceImpl;
import com.akif.payment.api.PaymentResult;
import com.akif.payment.api.PaymentService;
import com.akif.rental.api.RentalService;
import com.akif.rental.api.RentalSummaryDto;
import com.akif.shared.enums.CurrencyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DamageChargeService Unit Tests")
class DamageChargeServiceTest {

    @Mock
    private DamageReportRepository damageReportRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RentalService rentalService;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private DamageChargeServiceImpl damageChargeService;

    private DamageReport testDamageReport;
    private RentalSummaryDto testRentalSummary;

    @BeforeEach
    void setUp() {
        testDamageReport = DamageReport.builder()
                .id(1L)
                .rentalId(100L)
                .carId(200L)
                .customerUserId(300L)
                .customerEmail("customer@example.com")
                .severity(DamageSeverity.MODERATE)
                .category(DamageCategory.DENT)
                .status(DamageStatus.ASSESSED)
                .repairCostEstimate(new BigDecimal("500.00"))
                .customerLiability(new BigDecimal("350.00"))
                .build();

        testRentalSummary = new RentalSummaryDto(
                100L,
                200L,
                300L,
                "Toyota",
                "Corolla",
                "34ABC123",
                "customer@example.com",
                "John Doe",
                java.time.LocalDate.now(),
                java.time.LocalDate.now().plusDays(7),
                false,
                0
        );
    }

    @Nested
    @DisplayName("Create Damage Charge Tests")
    class CreateDamageChargeTests {

        @Test
        @DisplayName("Should create damage charge for assessed report")
        void shouldCreateDamageChargeForAssessedReport() {
            when(rentalService.getRentalSummaryById(testDamageReport.getRentalId()))
                    .thenReturn(testRentalSummary);
            when(damageReportRepository.save(any(DamageReport.class)))
                    .thenReturn(testDamageReport);

            Object result = damageChargeService.createDamageCharge(testDamageReport);

            verify(damageReportRepository).save(testDamageReport);
            assertThat(testDamageReport.getPaymentStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("Should throw exception for report with invalid status")
        void shouldThrowExceptionForInvalidStatus() {
            testDamageReport.updateStatus(DamageStatus.REPORTED);

            assertThatThrownBy(() -> damageChargeService.createDamageCharge(testDamageReport))
                    .isInstanceOf(DamageAssessmentException.class);

            verifyNoInteractions(rentalService);
            verifyNoInteractions(damageReportRepository);
        }

        @Test
        @DisplayName("Should throw exception for null customer liability")
        void shouldThrowExceptionForNullCustomerLiability() {
            testDamageReport.setCustomerLiability(null);

            assertThatThrownBy(() -> damageChargeService.createDamageCharge(testDamageReport))
                    .isInstanceOf(DamageAssessmentException.class)
                    .hasMessageContaining("Customer liability must be positive");
        }

        @Test
        @DisplayName("Should throw exception for zero customer liability")
        void shouldThrowExceptionForZeroCustomerLiability() {
            testDamageReport.setCustomerLiability(BigDecimal.ZERO);

            assertThatThrownBy(() -> damageChargeService.createDamageCharge(testDamageReport))
                    .isInstanceOf(DamageAssessmentException.class)
                    .hasMessageContaining("Customer liability must be positive");
        }
    }

    @Nested
    @DisplayName("Charge Damage Tests")
    class ChargeDamageTests {

        @Test
        @DisplayName("Should charge damage successfully by ID")
        void shouldChargeDamageSuccessfullyById() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L))
                    .thenReturn(Optional.of(testDamageReport));
            when(rentalService.getRentalSummaryById(testDamageReport.getRentalId()))
                    .thenReturn(testRentalSummary);
            when(paymentService.authorize(any(), any(), any()))
                    .thenReturn(new PaymentResult(true, "TXN-12345", "Success"));

            PaymentResult result = damageChargeService.chargeDamageById(1L);

            assertThat(result.success()).isTrue();
            assertThat(result.transactionId()).isEqualTo("TXN-12345");
            assertThat(testDamageReport.getStatus()).isEqualTo(DamageStatus.CHARGED);

            verify(eventPublisher).publishEvent(any(DamageChargedEvent.class));
        }

        @Test
        @DisplayName("Should return failure for invalid type")
        void shouldReturnFailureForInvalidType() {
            PaymentResult result = damageChargeService.chargeDamage("invalid");

            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("chargeDamageById");
        }

        @Test
        @DisplayName("Should handle payment failure")
        void shouldHandlePaymentFailure() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L))
                    .thenReturn(Optional.of(testDamageReport));
            when(rentalService.getRentalSummaryById(testDamageReport.getRentalId()))
                    .thenReturn(testRentalSummary);
            when(paymentService.authorize(any(), any(), any()))
                    .thenReturn(new PaymentResult(false, null, "Payment failed"));

            PaymentResult result = damageChargeService.chargeDamageById(1L);

            assertThat(result.success()).isFalse();
            assertThat(testDamageReport.getPaymentStatus()).isEqualTo("FAILED");
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Should throw exception when damage report not found")
        void shouldThrowExceptionWhenDamageReportNotFound() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> damageChargeService.chargeDamageById(999L))
                    .isInstanceOf(DamageAssessmentException.class)
                    .hasMessageContaining("Damage report not found");
        }
    }

    @Nested
    @DisplayName("Handle Failed Charge Tests")
    class HandleFailedChargeTests {

        @Test
        @DisplayName("Should handle failed charge with DamageReport")
        void shouldHandleFailedChargeWithDamageReport() {
            damageChargeService.handleFailedDamageCharge(testDamageReport);

            assertThat(testDamageReport.getPaymentStatus()).isEqualTo("FAILED");
            verify(damageReportRepository).save(testDamageReport);
        }

        @Test
        @DisplayName("Should handle failed charge with Long ID")
        void shouldHandleFailedChargeWithLongId() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L))
                    .thenReturn(Optional.of(testDamageReport));

            damageChargeService.handleFailedDamageCharge(1L);

            assertThat(testDamageReport.getPaymentStatus()).isEqualTo("FAILED");
            verify(damageReportRepository).save(testDamageReport);
        }
    }
}
