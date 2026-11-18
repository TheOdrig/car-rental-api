package com.akif.payment.internal.service.gateway.impl;

import com.akif.payment.api.PaymentResult;
import com.akif.payment.internal.config.StripeConfig;
import com.akif.payment.internal.exception.StripeIntegrationException;
import com.akif.payment.internal.service.gateway.ChargeCommand;
import com.akif.payment.internal.service.gateway.ChargeResult;
import com.akif.payment.internal.service.gateway.GatewayIntent;
import com.akif.payment.internal.service.gateway.GatewayIntentStatus;
import com.akif.payment.internal.service.gateway.IntentCommand;
import com.akif.payment.internal.service.gateway.PaymentGateway;
import com.akif.payment.internal.service.gateway.StripeAmounts;
import com.akif.shared.enums.CurrencyType;
import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentRetrieveParams;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class StripePaymentGateway implements PaymentGateway {

    private static final String METADATA_APP = "car-rental";

    private final StripeConfig stripeConfig;

    @Override
    public GatewayIntent createIntent(IntentCommand command, String idempotencyKey) {
        long minor = StripeAmounts.toMinorUnits(command.amount(), command.currency());
        log.info("[AUDIT] Stripe CREATE_INTENT | Rental: {} | Payment: {} | Amount: {} {} | Key: {}",
                command.rentalId(), command.paymentId(), command.amount(), command.currency(), idempotencyKey);
        try {
            String customerId = command.customerId();
            if (customerId == null) {
                customerId = createCustomer(command);
            }

            PaymentIntentCreateParams.Builder params = PaymentIntentCreateParams.builder()
                    .setAmount(minor)
                    .setCurrency(StripeAmounts.code(command.currency()))
                    .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                    .setCustomer(customerId)
                    .setSetupFutureUsage(PaymentIntentCreateParams.SetupFutureUsage.OFF_SESSION)
                    .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                            .build())
                    .putMetadata("payment_id", String.valueOf(command.paymentId()))
                    .putMetadata("rental_id", String.valueOf(command.rentalId()))
                    .putMetadata("app", METADATA_APP)
                    .putMetadata("env", stripeConfig.getMetadataEnvironment());
            if (command.description() != null) {
                params.setDescription(command.description());
            }

            PaymentIntent intent = PaymentIntent.create(params.build(), options(idempotencyKey));
            return toGatewayIntent(intent);
        } catch (StripeException e) {
            log.error("[AUDIT] Stripe CREATE_INTENT FAILED | Payment: {} | Code: {} | Request: {}",
                    command.paymentId(), e.getCode(), e.getRequestId());
            throw new StripeIntegrationException(e.getCode(), e.getUserMessage() != null ? e.getUserMessage() : "Stripe request failed", e);
        }
    }

    @Override
    public GatewayIntent retrieveIntent(String intentId) {
        try {
            return toGatewayIntent(PaymentIntent.retrieve(intentId, options(null)));
        } catch (StripeException e) {
            log.error("[AUDIT] Stripe RETRIEVE_INTENT FAILED | Intent: {} | Code: {} | Request: {}",
                    intentId, e.getCode(), e.getRequestId());
            throw new StripeIntegrationException(e.getCode(), "Stripe request failed", e);
        }
    }

    @Override
    public PaymentResult capture(String intentId, String idempotencyKey) {
        log.info("[AUDIT] Stripe CAPTURE | Intent: {} | Key: {}", intentId, idempotencyKey);
        try {
            PaymentIntent intent = new PaymentIntent();
            intent.setId(intentId);
            PaymentIntent captured = intent.capture(options(idempotencyKey));
            if (GatewayIntentStatus.fromStripe(captured.getStatus()) == GatewayIntentStatus.SUCCEEDED) {
                return PaymentResult.success(captured.getId(), "Payment captured");
            }
            return PaymentResult.failure("Capture did not complete, status: " + captured.getStatus());
        } catch (StripeException e) {
            return failure("CAPTURE", intentId, e);
        }
    }

    @Override
    public PaymentResult cancel(String intentId, String idempotencyKey) {
        log.info("[AUDIT] Stripe CANCEL | Intent: {} | Key: {}", intentId, idempotencyKey);
        try {
            PaymentIntent intent = new PaymentIntent();
            intent.setId(intentId);
            PaymentIntent canceled = intent.cancel(options(idempotencyKey));
            return PaymentResult.success(canceled.getId(), "Payment intent cancelled");
        } catch (StripeException e) {
            return failure("CANCEL", intentId, e);
        }
    }

    @Override
    public PaymentResult refund(String intentId, BigDecimal amount, CurrencyType currency, String idempotencyKey) {
        log.info("[AUDIT] Stripe REFUND | Intent: {} | Amount: {} {} | Key: {}", intentId, amount, currency, idempotencyKey);
        try {
            long minor = StripeAmounts.toMinorUnits(amount, currency);
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(intentId)
                    .setAmount(minor)
                    .build();
            Refund refund = Refund.create(params, options(idempotencyKey));
            return PaymentResult.success(refund.getId(), "Refund processed");
        } catch (IllegalArgumentException e) {
            return PaymentResult.failure(e.getMessage());
        } catch (StripeException e) {
            return failure("REFUND", intentId, e);
        }
    }

    @Override
    public ChargeResult chargeSavedMethod(ChargeCommand command, String idempotencyKey) {
        log.info("[AUDIT] Stripe OFF_SESSION_CHARGE | Rental: {} | Payment: {} | Amount: {} {} | Key: {}",
                command.rentalId(), command.paymentId(), command.amount(), command.currency(), idempotencyKey);
        try {
            long minor = StripeAmounts.toMinorUnits(command.amount(), command.currency());
            PaymentIntentCreateParams.Builder params = PaymentIntentCreateParams.builder()
                    .setAmount(minor)
                    .setCurrency(StripeAmounts.code(command.currency()))
                    .setCustomer(command.customerId())
                    .setPaymentMethod(command.paymentMethodId())
                    .setOffSession(true)
                    .setConfirm(true)
                    .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                            .build())
                    .putMetadata("payment_id", String.valueOf(command.paymentId()))
                    .putMetadata("rental_id", String.valueOf(command.rentalId()))
                    .putMetadata("app", METADATA_APP)
                    .putMetadata("env", stripeConfig.getMetadataEnvironment());
            if (command.description() != null) {
                params.setDescription(command.description());
            }
            PaymentIntent intent = PaymentIntent.create(params.build(), options(idempotencyKey));
            if (GatewayIntentStatus.fromStripe(intent.getStatus()) == GatewayIntentStatus.SUCCEEDED) {
                return ChargeResult.success(intent.getId(), "Charge succeeded");
            }
            return ChargeResult.declined("Charge requires customer action, status: " + intent.getStatus());
        } catch (IllegalArgumentException e) {
            return ChargeResult.declined(e.getMessage());
        } catch (StripeException e) {
            PaymentResult failed = failure("OFF_SESSION_CHARGE", String.valueOf(command.paymentId()), e);
            return new ChargeResult(failed, isDefinitiveRejection(e));
        }
    }

    @Override
    public long refundedMinor(String intentId) {
        try {
            PaymentIntentRetrieveParams params = PaymentIntentRetrieveParams.builder()
                    .addExpand("latest_charge")
                    .build();
            PaymentIntent intent = PaymentIntent.retrieve(intentId, params, options(null));
            Charge charge = intent.getLatestChargeObject();
            if (charge == null || charge.getAmountRefunded() == null) {
                return 0L;
            }
            return charge.getAmountRefunded();
        } catch (StripeException e) {
            log.error("[AUDIT] Stripe READ_REFUNDED FAILED | Intent: {} | Code: {} | Request: {}",
                    intentId, e.getCode(), e.getRequestId());
            throw new StripeIntegrationException(e.getCode(), "Stripe request failed", e);
        }
    }

    private static boolean isDefinitiveRejection(StripeException e) {
        Integer status = e.getStatusCode();
        if (e instanceof CardException) {
            return true;
        }
        return status != null && status >= 400 && status < 500 && status != 409 && status != 429;
    }

    private String createCustomer(IntentCommand command) throws StripeException {
        CustomerCreateParams.Builder params = CustomerCreateParams.builder()
                .putMetadata("rental_id", String.valueOf(command.rentalId()));
        if (command.customerEmail() != null) {
            params.setEmail(command.customerEmail());
        }
        Customer customer = Customer.create(params.build(), options(command.customerIdempotencyKey()));
        return customer.getId();
    }

    private RequestOptions options(String idempotencyKey) {
        RequestOptions.RequestOptionsBuilder builder = stripeConfig.requestOptions();
        if (idempotencyKey != null) {
            builder.setIdempotencyKey(idempotencyKey);
        }
        return builder.build();
    }

    private GatewayIntent toGatewayIntent(PaymentIntent intent) {
        return new GatewayIntent(
                intent.getId(),
                intent.getClientSecret(),
                GatewayIntentStatus.fromStripe(intent.getStatus()),
                intent.getPaymentMethod(),
                intent.getCustomer(),
                intent.getAmount() != null ? intent.getAmount() : 0L,
                intent.getCurrency());
    }

    private PaymentResult failure(String operation, String reference, StripeException e) {
        log.error("[AUDIT] Stripe {} FAILED | Ref: {} | Code: {} | Request: {}",
                operation, reference, e.getCode(), e.getRequestId());
        String reason = e.getUserMessage() != null ? e.getUserMessage() : "Stripe request failed";
        return PaymentResult.failure(reason);
    }
}
