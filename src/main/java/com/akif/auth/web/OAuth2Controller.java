package com.akif.auth.web;

import com.akif.auth.internal.oauth2.dto.response.LinkAccountResponse;
import com.akif.auth.AuthResponse;
import com.akif.auth.domain.User;
import com.akif.auth.repository.UserRepository;
import com.akif.auth.internal.oauth2.IOAuth2AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "OAuth2", description = "OAuth2 Social Login endpoints")
public class OAuth2Controller {

    private final IOAuth2AuthService oAuth2AuthService;
    private final UserRepository userRepository;

    @GetMapping("/authorize/{provider}")
    @Operation(summary = "Initiate OAuth2 login", 
               description = "Redirects user to the OAuth2 provider's authorization page")
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "Redirect to provider"),
        @ApiResponse(responseCode = "400", description = "Invalid provider")
    })
    public void authorize(
            @Parameter(description = "OAuth2 provider (google, github)")
            @PathVariable String provider,
            HttpServletResponse response) throws IOException {
        
        log.debug("Initiating OAuth2 authorization for provider: {}", provider);
        String authorizationUrl = oAuth2AuthService.getAuthorizationUrl(provider);
        response.sendRedirect(authorizationUrl);
    }


    @GetMapping("/callback/{provider}")
    @Operation(summary = "OAuth2 callback", 
               description = "Handles the callback from OAuth2 provider after user authorization")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authentication successful"),
        @ApiResponse(responseCode = "400", description = "Invalid state or code"),
        @ApiResponse(responseCode = "401", description = "Authentication failed"),
        @ApiResponse(responseCode = "503", description = "Provider unavailable")
    })
    public ResponseEntity<AuthResponse> callback(
            @Parameter(description = "OAuth2 provider (google, github)")
            @PathVariable String provider,
            @Parameter(description = "Authorization code from provider")
            @RequestParam String code,
            @Parameter(description = "State parameter for CSRF protection")
            @RequestParam String state) {
        
        log.debug("Processing OAuth2 callback for provider: {}", provider);
        AuthResponse response = oAuth2AuthService.processOAuth2Callback(provider, code, state);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/link/{provider}")
    @Operation(summary = "Link social account", 
               description = "Links a social account to the authenticated user's account")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account linked successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid state or code"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "409", description = "Account already linked to another user")
    })
    public ResponseEntity<LinkAccountResponse> linkAccount(
            @Parameter(description = "OAuth2 provider (google, github)")
            @PathVariable String provider,
            @Parameter(description = "Authorization code from provider")
            @RequestParam String code,
            @Parameter(description = "State parameter for CSRF protection")
            @RequestParam String state,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Linking {} account for user: {}", provider, userDetails.getUsername());
        
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LinkAccountResponse response = oAuth2AuthService.linkSocialAccount(
                provider, code, state, user.getId());
        return ResponseEntity.ok(response);
    }
}
