package com.akif.controller;

import com.akif.exception.WebhookSignatureException;
import com.akif.service.webhook.StripeWebhookHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
               description = "Receives and processes webhook events from Stripe")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Webhook processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid webhook signature")
    })
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            HttpServletRequest request) {
        
        log.info("Received Stripe webhook");

        String signature = request.getHeader("Stripe-Signature");
        if (signature == null || signature.isEmpty()) {
            log.error("Missing Stripe-Signature header");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Missing signature");
        }

        try {
            webhookHandler.handleWebhookEvent(payload, signature);
            return ResponseEntity.ok("Webhook processed successfully");
            
        } catch (WebhookSignatureException e) {
            log.error("Invalid webhook signature", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid signature");
                    
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook");
        }
    }
}
