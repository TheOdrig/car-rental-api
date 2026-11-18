package com.akif.payment.web;

import com.akif.payment.internal.exception.WebhookSignatureException;
import com.akif.payment.internal.webhook.StripeWebhookHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Stripe webhook endpoints")
public class StripeWebhookController {

    private final StripeWebhookHandler webhookHandler;

    @PostMapping("/stripe")
    @Operation(summary = "Handle Stripe webhook events",
               description = "Receives events from Stripe, verifies the Stripe-Signature header and processes each event id once")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Webhook processed successfully"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid webhook signature"),
            @ApiResponse(responseCode = "500", description = "Processing failed; Stripe will retry")
    })
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {

        if (signature == null || signature.isBlank()) {
            log.warn("Stripe webhook rejected: missing Stripe-Signature header");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing signature");
        }

        try {
            webhookHandler.handleWebhookEvent(payload, signature);
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (WebhookSignatureException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Stripe webhook processing failed: {}", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
        }
    }
}
