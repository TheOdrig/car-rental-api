package com.akif.service.oauth2;

import com.akif.config.OAuth2Properties;
import com.akif.enums.OAuth2ErrorType;
import com.akif.exception.OAuth2AuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@Slf4j
public class OAuth2StateService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int RANDOM_BYTES_LENGTH = 16;
    private static final String DELIMITER = ".";

    private final OAuth2Properties oAuth2Properties;
    private final SecureRandom secureRandom;

    public OAuth2StateService(OAuth2Properties oAuth2Properties) {
        this.oAuth2Properties = oAuth2Properties;
        this.secureRandom = new SecureRandom();
    }

    public String generateState() {
        long timestamp = Instant.now().getEpochSecond();
        String random = generateRandomString();
        String data = timestamp + DELIMITER + random;
        String signature = sign(data);

        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((data + DELIMITER + signature).getBytes(StandardCharsets.UTF_8));
    }


    public boolean validateState(String state) {
        if (state == null || state.isBlank()) {
            log.warn("State parameter is null or empty");
            throw new OAuth2AuthenticationException(OAuth2ErrorType.INVALID_STATE);
        }

        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(state),
                    StandardCharsets.UTF_8
            );

            String[] parts = decoded.split("\\" + DELIMITER);
            if (parts.length != 3) {
                log.warn("Invalid state format: expected 3 parts, got {}", parts.length);
                throw new OAuth2AuthenticationException(OAuth2ErrorType.INVALID_STATE);
            }

            String timestamp = parts[0];
            String random = parts[1];
            String signature = parts[2];

            String data = timestamp + DELIMITER + random;
            String expectedSignature = sign(data);
            if (!constantTimeEquals(signature, expectedSignature)) {
                log.warn("State signature verification failed");
                throw new OAuth2AuthenticationException(OAuth2ErrorType.INVALID_STATE);
            }

            long stateTimestamp = Long.parseLong(timestamp);
            long currentTimestamp = Instant.now().getEpochSecond();
            long expirationSeconds = oAuth2Properties.getState().getExpirationMinutes() * 60L;

            if (currentTimestamp - stateTimestamp > expirationSeconds) {
                log.warn("State parameter has expired");
                throw new OAuth2AuthenticationException(OAuth2ErrorType.INVALID_STATE);
            }

            log.debug("State parameter validated successfully");
            return true;
        } catch (IllegalArgumentException e) {
            log.warn("Failed to decode state parameter: {}", e.getMessage());
            throw new OAuth2AuthenticationException(OAuth2ErrorType.INVALID_STATE);
        }
    }

    private String generateRandomString() {
        byte[] randomBytes = new byte[RANDOM_BYTES_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(
                    oAuth2Properties.getState().getSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to sign state data: {}", e.getMessage());
            throw new RuntimeException("Failed to generate state signature", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        if (aBytes.length != bBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }
}
