package com.akif.e2e.infrastructure;

import com.akif.dto.request.RentalRequestDto;
import com.akif.dto.response.RentalResponseDto;
import com.akif.shared.enums.Role;
import com.akif.model.User;
import com.akif.scheduler.ReminderScheduler;
import com.akif.shared.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class E2ETestBase {
    
    @Autowired
    protected MockMvc mockMvc;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    @Autowired
    protected JwtTokenProvider tokenProvider;
    
    @Autowired
    protected TestEventCaptor eventCaptor;

    @MockitoBean
    protected ReminderScheduler reminderScheduler;

    @BeforeEach
    public void setUp() {
        eventCaptor.clear();
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
        return tokenProvider.generateAccessToken(authentication);
    }

    protected Long createAndGetRentalId(RentalRequestDto request, String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/rentals/request")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        
        String responseBody = result.getResponse().getContentAsString();
        RentalResponseDto response = objectMapper.readValue(responseBody, RentalResponseDto.class);
        return response.getId();
    }
}
