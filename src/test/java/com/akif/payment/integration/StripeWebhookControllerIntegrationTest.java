package com.akif.payment.integration;

import com.akif.payment.internal.exception.WebhookSignatureException;
import com.akif.payment.internal.webhook.StripeWebhookHandler;
import com.akif.starter.CarGalleryProjectApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("StripeWebhookController Integration Tests")
class StripeWebhookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StripeWebhookHandler webhookHandler;

    private String validPayload;
    private String validSignature;

    @BeforeEach
    void setUp() {
        validPayload = "{\"id\":\"evt_test_123\",\"type\":\"checkout.session.completed\"}";
        validSignature = "t=1234567890,v1=test_signature";
    }

    @Nested
    @DisplayName("POST /api/webhooks/stripe")
    class HandleWebhook {

        @Test
        @DisplayName("Should return 200 when webhook is processed successfully")
        void shouldReturn200WhenWebhookProcessedSuccessfully() throws Exception {
            doNothing().when(webhookHandler).handleWebhookEvent(anyString(), anyString());

            mockMvc.perform(post("/api/webhooks/stripe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Stripe-Signature", validSignature)
                            .content(validPayload))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Webhook processed successfully"));
        }

        @Test
        @DisplayName("Should return 400 when signature is missing")
        void shouldReturn400WhenSignatureMissing() throws Exception {
            mockMvc.perform(post("/api/webhooks/stripe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validPayload))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Missing signature"));
        }

        @Test
        @DisplayName("Should return 400 when signature is empty")
        void shouldReturn400WhenSignatureEmpty() throws Exception {
            mockMvc.perform(post("/api/webhooks/stripe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Stripe-Signature", "")
                            .content(validPayload))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Missing signature"));
        }

        @Test
        @DisplayName("Should return 400 when signature verification fails")
        void shouldReturn400WhenSignatureVerificationFails() throws Exception {
            doThrow(new WebhookSignatureException("evt_test_123", new Exception("Invalid signature")))
                    .when(webhookHandler).handleWebhookEvent(anyString(), anyString());

            mockMvc.perform(post("/api/webhooks/stripe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Stripe-Signature", "invalid_signature")
                            .content(validPayload))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Invalid signature"));
        }

        @Test
        @DisplayName("Should return 500 when processing fails with unexpected error")
        void shouldReturn500WhenProcessingFails() throws Exception {
            doThrow(new RuntimeException("Unexpected error"))
                    .when(webhookHandler).handleWebhookEvent(anyString(), anyString());

            mockMvc.perform(post("/api/webhooks/stripe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Stripe-Signature", validSignature)
                            .content(validPayload))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string("Error processing webhook"));
        }

        @Test
        @DisplayName("Should accept webhook with valid payload and signature")
        void shouldAcceptWebhookWithValidPayloadAndSignature() throws Exception {
            doNothing().when(webhookHandler).handleWebhookEvent(validPayload, validSignature);

            mockMvc.perform(post("/api/webhooks/stripe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Stripe-Signature", validSignature)
                            .content(validPayload))
                    .andExpect(status().isOk());
        }
    }
}
