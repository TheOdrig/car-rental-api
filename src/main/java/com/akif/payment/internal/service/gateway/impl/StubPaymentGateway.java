package com.akif.payment.internal.service.gateway.impl;

import com.akif.payment.api.PaymentResult;
import com.akif.payment.internal.service.gateway.ChargeCommand;
import com.akif.payment.internal.service.gateway.ChargeResult;
import com.akif.payment.internal.service.gateway.GatewayIntent;
import com.akif.payment.internal.service.gateway.GatewayIntentStatus;
import com.akif.payment.internal.service.gateway.IntentCommand;
import com.akif.payment.internal.service.gateway.PaymentGateway;
import com.akif.payment.internal.service.gateway.StripeAmounts;
import com.akif.shared.enums.CurrencyType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@Profile({"dev", "test"})
public class StubPaymentGateway implements PaymentGateway {

    static final String STUB_PREFIX = "stub_pi_";

    private final Map<String, GatewayIntentStatus> intents = new ConcurrentHashMap<>();
    private final Map<String, GatewayIntent> created = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> refunds = new ConcurrentHashMap<>();
    private final Map<String, Long> refundedMinor = new ConcurrentHashMap<>();

    @PostConstruct
    void announce() {
        log.warn("[STUB] StubPaymentGateway is active: no real payment is made and every intent is reported as already authorized");
    }

    @Override
    public GatewayIntent createIntent(IntentCommand command, String idempotencyKey) {
        long minor = StripeAmounts.toMinorUnits(command.amount(), command.currency());
        String id = STUB_PREFIX + stableId(idempotencyKey);
        return created.computeIfAbsent(id, key -> {
            intents.put(key, GatewayIntentStatus.REQUIRES_CAPTURE);
            return new GatewayIntent(key, "stub_secret_" + key, GatewayIntentStatus.REQUIRES_CAPTURE,
                    "stub_pm_card", command.customerId() != null ? command.customerId() : "stub_cus_" + command.rentalId(),
                    minor, StripeAmounts.code(command.currency()));
        });
    }

    @Override
    public GatewayIntent retrieveIntent(String intentId) {
        GatewayIntent base = created.get(intentId);
        GatewayIntentStatus status = intents.getOrDefault(intentId, GatewayIntentStatus.REQUIRES_CAPTURE);
        if (base == null) {
            return new GatewayIntent(intentId, "stub_secret_" + intentId, status, "stub_pm_card", "stub_cus", 0L, "usd");
        }
        return new GatewayIntent(base.id(), base.clientSecret(), status, base.paymentMethodId(),
                base.customerId(), base.amountMinor(), base.currency());
    }

    @Override
    public Optional<GatewayIntent> findIntentByPayment(Long paymentId) {
        return Optional.empty();
    }

    @Override
    public PaymentResult capture(String intentId, String idempotencyKey) {
        intents.put(intentId, GatewayIntentStatus.SUCCEEDED);
        return PaymentResult.success(intentId, "Payment captured (STUB)");
    }

    @Override
    public PaymentResult cancel(String intentId, String idempotencyKey) {
        intents.put(intentId, GatewayIntentStatus.CANCELED);
        return PaymentResult.success(intentId, "Payment intent cancelled (STUB)");
    }

    @Override
    public PaymentResult refund(String intentId, BigDecimal amount, CurrencyType currency, String idempotencyKey) {
        String refundId = "stub_re_" + stableId(idempotencyKey);
        if (refunds.putIfAbsent(refundId, amount) == null) {
            refundedMinor.merge(intentId, StripeAmounts.toMinorUnits(amount, currency), Long::sum);
        }
        return PaymentResult.success(refundId, "Payment refunded (STUB)");
    }

    @Override
    public long refundedMinor(String intentId) {
        return refundedMinor.getOrDefault(intentId, 0L);
    }

    @Override
    public ChargeResult chargeSavedMethod(ChargeCommand command, String idempotencyKey) {
        String id = STUB_PREFIX + stableId(idempotencyKey);
        intents.put(id, GatewayIntentStatus.SUCCEEDED);
        return ChargeResult.success(id, "Charge succeeded (STUB)");
    }

    private String stableId(String key) {
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "").substring(0, 16);
    }
}
