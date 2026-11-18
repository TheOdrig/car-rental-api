package com.akif.e2e.infrastructure;

import com.akif.auth.domain.User;
import com.akif.payment.api.PaymentService;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.api.RentalRequest;
import com.akif.rental.internal.config.RentalProperties;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.rental.internal.scheduler.ReminderScheduler;
import com.akif.shared.enums.Role;
import com.akif.shared.security.JwtTokenService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import com.akif.testsupport.MutableClock;
import com.akif.testsupport.TestClockConfiguration;
import com.akif.testsupport.TestDatabase;
import com.akif.testsupport.TestcontainersConfiguration;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class E2ETestBase {
    
    @Autowired
    protected MockMvc mockMvc;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    @Autowired
    protected JwtTokenService tokenProvider;
    
    @Autowired
    protected TestEventCaptor eventCaptor;

    @Autowired
    protected PaymentService paymentService;

    @Autowired
    protected MutableClock clock;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected RentalRepository rentalRepository;

    @Autowired
    protected RentalProperties rentalProperties;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    protected ReminderScheduler reminderScheduler;

    protected void payRental(Long rentalId) {
        paymentService.getPaymentByRentalId(rentalId)
                .ifPresent(payment -> paymentService.createOrGetIntentForRental(rentalId));
    }

    protected void moveStartToToday(Long rentalId) {
        jdbcTemplate.update("UPDATE car_rental.rentals SET start_date = ? WHERE id = ? AND start_date > ?",
                LocalDate.now(clock), rentalId, LocalDate.now(clock));
    }

    protected void advanceClockToPaymentWindowOf(Long rentalId) {
        LocalDate start = rentalRepository.findById(rentalId).orElseThrow().getStartDate();
        LocalDate opens = start.minusDays(rentalProperties.getPaymentWindowDays());
        long days = ChronoUnit.DAYS.between(LocalDate.now(clock), opens);
        if (days > 0) {
            clock.advance(Duration.ofDays(days));
        }
    }

    @BeforeEach
    public void setUp() {
        resetState();
    }

    @AfterEach
    public void tearDownState() {
        resetState();
    }

    private void resetState() {
        SecurityContextHolder.clearContext();
        eventCaptor.clear();
        clock.reset();
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        TestDatabase.truncateAll(jdbcTemplate);
    }

    protected String generateUserToken(User user) {
        return generateToken(user, Set.of(Role.USER));
    }

    protected String generateAdminToken(User user) {
        return generateToken(user, Set.of(Role.ADMIN));
    }

    private String generateToken(User user, Set<Role> roles) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .collect(Collectors.toList())
        );
        return tokenProvider.generateAccessToken(authentication, user.getId());
    }

    protected Long createAndGetRentalId(RentalRequest request, String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/rentals/request")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        
        String responseBody = result.getResponse().getContentAsString();
        RentalResponse response = objectMapper.readValue(responseBody, RentalResponse.class);
        return response.id();
    }
}
