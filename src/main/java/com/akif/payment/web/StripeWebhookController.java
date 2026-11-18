package com.akif.payment.web;

import com.akif.payment.internal.config.StripeConfig;
import com.akif.payment.internal.exception.WebhookDeliveryException;
import com.akif.payment.internal.exception.WebhookSignatureException;
import com.akif.payment.internal.webhook.StripeWebhookHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Stripe webhook endpoints")
public class StripeWebhookController {

    private final StripeWebhookHandler webhookHandler;
    private final StripeConfig stripeConfig;

    @PostMapping("/stripe")
    @Operation(summary = "Handle Stripe webhook events",
               description = "Receives events from Stripe, verifies the Stripe-Signature header and processes each event id once")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Webhook processed successfully"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid webhook signature"),
            @ApiResponse(responseCode = "413", description = "Request body exceeds the configured limit"),
            @ApiResponse(responseCode = "500", description = "Processing failed; Stripe will retry")
    })
    public ResponseEntity<String> handleStripeWebhook(
            HttpServletRequest request,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) throws IOException {

        long limit = stripeConfig.getWebhookMaxBodyBytes();
        if (request.getContentLengthLong() > limit) {
            log.warn("Stripe webhook rejected: declared body exceeds {} bytes", limit);
            throw WebhookDeliveryException.payloadTooLarge(limit);
        }

        if (signature == null || signature.isBlank()) {
            log.warn("Stripe webhook rejected: missing Stripe-Signature header");
            throw WebhookDeliveryException.missingSignature();
        }

        String payload = readBounded(request.getInputStream(), limit);
        if (payload == null) {
            log.warn("Stripe webhook rejected: body exceeds {} bytes", limit);
            throw WebhookDeliveryException.payloadTooLarge(limit);
        }

        try {
            webhookHandler.handleWebhookEvent(payload, signature);
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (WebhookSignatureException e) {
            throw e;
        } catch (Exception e) {
            log.error("Stripe webhook processing failed: {}", e.getClass().getSimpleName());
            throw WebhookDeliveryException.processingFailed();
        }
    }

    private String readBounded(InputStream in, long limit) throws IOException {
        byte[] bytes = in.readNBytes((int) limit + 1);
        if (bytes.length > limit) {
            return null;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
