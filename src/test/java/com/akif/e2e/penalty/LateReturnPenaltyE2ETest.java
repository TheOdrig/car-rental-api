package com.akif.e2e.penalty;

import com.akif.dto.request.PenaltyWaiverRequestDto;
import com.akif.dto.request.ReturnRequestDto;
import com.akif.e2e.infrastructure.E2ETestBase;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.e2e.infrastructure.TestFixtures;
import com.akif.enums.LateReturnStatus;
import com.akif.enums.RentalStatus;
import com.akif.event.GracePeriodWarningEvent;
import com.akif.event.LateReturnNotificationEvent;
import com.akif.event.PenaltySummaryEvent;
import com.akif.event.SeverelyLateNotificationEvent;
import com.akif.model.Car;
import com.akif.model.PenaltyWaiver;
import com.akif.model.Rental;
import com.akif.model.User;
import com.akif.repository.CarRepository;
import com.akif.repository.PenaltyWaiverRepository;
import com.akif.repository.RentalRepository;
import com.akif.repository.UserRepository;
import com.akif.service.detection.ILateReturnDetectionService;
import com.akif.service.gateway.IPaymentGateway;
import com.akif.service.gateway.PaymentResult;
import com.akif.starter.CarGalleryProjectApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@DisplayName("Late Return Penalty E2E Tests")
class LateReturnPenaltyE2ETest extends E2ETestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private PenaltyWaiverRepository penaltyWaiverRepository;

    @Autowired
    private ILateReturnDetectionService lateReturnDetectionService;

    @MockitoSpyBean
    private IPaymentGateway paymentGateway;

    private Rental createPastRental(User user, Car car, int daysAgo) {
        LocalDate pastStart = LocalDate.now().minusDays(daysAgo + 3);
        LocalDate pastEnd = LocalDate.now().minusDays(daysAgo);
        
        return Rental.builder()
                .user(user)
                .car(car)
                .startDate(pastStart)
                .endDate(pastEnd)
                .days(3)
                .currency(TestFixtures.DEFAULT_CURRENCY)
                .dailyPrice(car.getPrice())
                .totalPrice(car.getPrice().multiply(new BigDecimal("3")))
                .status(RentalStatus.IN_USE)
                .penaltyPaid(false)
                .isDeleted(false)
                .build();
    }
    
    private String generateAdminTokenWithEmail(User admin) {
        return generateAdminToken(admin);
    }

    @Test
    @DisplayName("Should complete full late return flow: rental → late detection → penalty → payment")
    void shouldCompleteFullLateReturnFlow() throws Exception {
        User testUser = userRepository.save(TestDataBuilder.createTestUser());
        User testAdmin = userRepository.save(TestDataBuilder.createTestAdmin());
        String adminToken = generateAdminToken(testAdmin);

        Car testCar = carRepository.save(TestDataBuilder.createAvailableCar());
        
        Rental rental = createPastRental(testUser, testCar, 2);
        rental = rentalRepository.save(rental);
        Long rentalId = rental.getId();

        eventCaptor.clear();
        lateReturnDetectionService.detectLateReturns();

        rental = rentalRepository.findByIdAndIsDeletedFalse(rentalId)
                .orElseThrow(() -> new AssertionError("Rental not found: " + rentalId));
        
        assertThat(rental.getLateReturnStatus()).isIn(LateReturnStatus.LATE, LateReturnStatus.SEVERELY_LATE);
        assertThat(rental.getLateDetectedAt()).isNotNull();
        assertThat(rental.getLateHours()).isGreaterThan(0);

        List<LateReturnNotificationEvent> lateEvents = eventCaptor.getEventsOfType(LateReturnNotificationEvent.class);
        List<SeverelyLateNotificationEvent> severeEvents = eventCaptor.getEventsOfType(SeverelyLateNotificationEvent.class);
        assertThat(lateEvents.size() + severeEvents.size()).isGreaterThan(0);

        when(paymentGateway.authorize(any(BigDecimal.class), any(), anyString()))
                .thenReturn(new PaymentResult(true, "auth_txn_123", "Penalty authorized"));
        when(paymentGateway.capture(anyString(), any(BigDecimal.class)))
                .thenReturn(new PaymentResult(true, "penalty_txn_123", "Penalty captured"));

        eventCaptor.clear();
        ReturnRequestDto returnRequest = ReturnRequestDto.builder()
                .notes("Late return")
                .build();

        mockMvc.perform(post("/api/rentals/{id}/return", rentalId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(returnRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(RentalStatus.RETURNED.getDisplayName()));

        rental = rentalRepository.findByIdAndIsDeletedFalse(rentalId)
                .orElseThrow(() -> new AssertionError("Rental not found: " + rentalId));
        
        assertThat(rental.getPenaltyAmount()).isNotNull();
        assertThat(rental.getPenaltyAmount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(rental.getActualReturnTime()).isNotNull();

        List<PenaltySummaryEvent> summaryEvents = eventCaptor.getEventsOfType(PenaltySummaryEvent.class);
        assertThat(summaryEvents).isNotEmpty();
    }

    @Test
    @DisplayName("Should detect grace period status for rentals within grace period")
    void shouldDetectGracePeriodStatus() throws Exception {
        User testUser = userRepository.save(TestDataBuilder.createTestUser());
        Car testCar = carRepository.save(TestDataBuilder.createAvailableCar());

        LocalDate yesterday = LocalDate.now().minusDays(1);
        Rental rental = Rental.builder()
                .user(testUser)
                .car(testCar)
                .startDate(yesterday.minusDays(3))
                .endDate(yesterday)
                .days(3)
                .currency(TestFixtures.DEFAULT_CURRENCY)
                .dailyPrice(testCar.getPrice())
                .totalPrice(testCar.getPrice().multiply(new BigDecimal("3")))
                .status(RentalStatus.IN_USE)
                .penaltyPaid(false)
                .isDeleted(false)
                .build();
        rental = rentalRepository.save(rental);
        Long rentalId = rental.getId();

        eventCaptor.clear();
        lateReturnDetectionService.detectLateReturns();

        rental = rentalRepository.findByIdAndIsDeletedFalse(rentalId)
                .orElseThrow(() -> new AssertionError("Rental not found: " + rentalId));

        assertThat(rental.getLateReturnStatus()).isIn(LateReturnStatus.GRACE_PERIOD, LateReturnStatus.LATE);

        boolean hasEvent = !eventCaptor.getEventsOfType(GracePeriodWarningEvent.class).isEmpty() ||
                          !eventCaptor.getEventsOfType(LateReturnNotificationEvent.class).isEmpty();
        assertThat(hasEvent).isTrue();
    }

    @Test
    @DisplayName("Should detect severely late status for rentals 24+ hours late")
    void shouldDetectSeverelyLateStatus() throws Exception {
        User testUser = userRepository.save(TestDataBuilder.createTestUser());
        Car testCar = carRepository.save(TestDataBuilder.createAvailableCar());
        
        Rental rental = createPastRental(testUser, testCar, 3);
        rental = rentalRepository.save(rental);
        Long rentalId = rental.getId();

        eventCaptor.clear();
        lateReturnDetectionService.detectLateReturns();

        rental = rentalRepository.findByIdAndIsDeletedFalse(rentalId)
                .orElseThrow(() -> new AssertionError("Rental not found: " + rentalId));
        
        assertThat(rental.getLateReturnStatus()).isEqualTo(LateReturnStatus.SEVERELY_LATE);
        assertThat(rental.getLateHours()).isGreaterThan(24);

        List<SeverelyLateNotificationEvent> severeEvents = eventCaptor.getEventsOfType(SeverelyLateNotificationEvent.class);
        assertThat(severeEvents).isNotEmpty();
    }

    @Test
    @DisplayName("Should charge penalty payment through payment gateway")
    void shouldChargePenaltyThroughGateway() throws Exception {
        User testUser = userRepository.save(TestDataBuilder.createTestUser());
        User testAdmin = userRepository.save(TestDataBuilder.createTestAdmin());
        String adminToken = generateAdminToken(testAdmin);

        Car testCar = carRepository.save(TestDataBuilder.createAvailableCar());
        
        Rental rental = createPastRental(testUser, testCar, 2);
        rental = rentalRepository.save(rental);
        Long rentalId = rental.getId();

        lateReturnDetectionService.detectLateReturns();

        when(paymentGateway.authorize(any(BigDecimal.class), any(), anyString()))
                .thenReturn(new PaymentResult(true, "auth_txn_456", "Penalty authorized"));
        when(paymentGateway.capture(anyString(), any(BigDecimal.class)))
                .thenReturn(new PaymentResult(true, "penalty_txn_456", "Penalty captured"));

        ReturnRequestDto returnRequest = ReturnRequestDto.builder()
                .notes("Late return")
                .build();

        mockMvc.perform(post("/api/rentals/{id}/return", rentalId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(returnRequest)))
                .andExpect(status().isOk());

        verify(paymentGateway).capture(anyString(), any(BigDecimal.class));

        rental = rentalRepository.findByIdAndIsDeletedFalse(rentalId)
                .orElseThrow(() -> new AssertionError("Rental not found: " + rentalId));
        
        assertThat(rental.getPenaltyPaid()).isTrue();
    }

    @Test
    @DisplayName("Should waive penalty and process refund")
    void shouldWaivePenaltyAndProcessRefund() throws Exception {
        User testUser = userRepository.save(TestDataBuilder.createTestUser());
        User testAdmin = userRepository.save(TestDataBuilder.createTestAdmin());
        String adminToken = generateAdminToken(testAdmin);

        Car testCar = carRepository.save(TestDataBuilder.createAvailableCar());
        
        Rental rental = createPastRental(testUser, testCar, 2);
        rental = rentalRepository.save(rental);
        Long rentalId = rental.getId();

        lateReturnDetectionService.detectLateReturns();

        when(paymentGateway.authorize(any(BigDecimal.class), any(), anyString()))
                .thenReturn(new PaymentResult(true, "auth_txn_789", "Penalty authorized"));
        when(paymentGateway.capture(anyString(), any(BigDecimal.class)))
                .thenReturn(new PaymentResult(true, "penalty_txn_789", "Penalty captured"));
        when(paymentGateway.refund(anyString(), any(BigDecimal.class)))
                .thenReturn(new PaymentResult(true, "refund_txn_789", "Refund processed"));

        ReturnRequestDto returnRequest = ReturnRequestDto.builder()
                .notes("Late return")
                .build();

        mockMvc.perform(post("/api/rentals/{id}/return", rentalId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(returnRequest)))
                .andExpect(status().isOk());

        rental = rentalRepository.findByIdAndIsDeletedFalse(rentalId)
                .orElseThrow(() -> new AssertionError("Rental not found: " + rentalId));
        
        BigDecimal penaltyAmount = rental.getPenaltyAmount();
        assertThat(penaltyAmount).isGreaterThan(BigDecimal.ZERO);

        PenaltyWaiverRequestDto waiverRequest = PenaltyWaiverRequestDto.builder()
                .waiverAmount(penaltyAmount)
                .reason("Customer complaint - first time late")
                .build();

        mockMvc.perform(post("/api/admin/rentals/{id}/penalty/waive", rentalId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(waiverRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waivedAmount").value(penaltyAmount.doubleValue()))
                .andExpect(jsonPath("$.remainingPenalty").value(0.0));

        List<PenaltyWaiver> waivers = penaltyWaiverRepository.findByRentalIdAndIsDeletedFalse(rentalId);
        assertThat(waivers).hasSize(1);
        
        PenaltyWaiver waiver = waivers.get(0);
        assertThat(waiver.getWaivedAmount()).isEqualByComparingTo(penaltyAmount);
        assertThat(waiver.getReason()).isEqualTo("Customer complaint - first time late");
        assertThat(waiver.getAdminId()).isEqualTo(testAdmin.getId());

        verify(paymentGateway).refund(anyString(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Should retrieve penalty history")
    void shouldRetrievePenaltyHistory() throws Exception {
        User testUser = userRepository.save(TestDataBuilder.createTestUser());
        User testAdmin = userRepository.save(TestDataBuilder.createTestAdmin());
        String adminToken = generateAdminToken(testAdmin);

        Car testCar = carRepository.save(TestDataBuilder.createAvailableCar());
        
        Rental rental = createPastRental(testUser, testCar, 2);
        rental = rentalRepository.save(rental);
        Long rentalId = rental.getId();

        lateReturnDetectionService.detectLateReturns();

        when(paymentGateway.authorize(any(BigDecimal.class), any(), anyString()))
                .thenReturn(new PaymentResult(true, "auth_txn_999", "Penalty authorized"));
        when(paymentGateway.capture(anyString(), any(BigDecimal.class)))
                .thenReturn(new PaymentResult(true, "penalty_txn_999", "Penalty captured"));

        ReturnRequestDto returnRequest = ReturnRequestDto.builder()
                .notes("Late return")
                .build();

        mockMvc.perform(post("/api/rentals/{id}/return", rentalId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(returnRequest)))
                .andExpect(status().isOk());

        rental = rentalRepository.findByIdAndIsDeletedFalse(rentalId)
                .orElseThrow(() -> new AssertionError("Rental not found: " + rentalId));

        BigDecimal partialWaiver = rental.getPenaltyAmount().multiply(new BigDecimal("0.5"));
        
        PenaltyWaiverRequestDto waiverRequest = PenaltyWaiverRequestDto.builder()
                .waiverAmount(partialWaiver)
                .reason("Partial waiver - 50% discount")
                .build();

        mockMvc.perform(post("/api/admin/rentals/{id}/penalty/waive", rentalId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(waiverRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/rentals/{id}/penalty/history", rentalId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].waivedAmount").value(partialWaiver.doubleValue()))
                .andExpect(jsonPath("$[0].reason").value("Partial waiver - 50% discount"));
    }
}
