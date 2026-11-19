package com.akif.payment.internal.service.reconciliation;

import com.akif.payment.domain.Payment;
import com.akif.payment.domain.enums.DiscrepancyType;
import com.akif.payment.internal.config.StripeConfig;
import com.akif.payment.internal.dto.Discrepancy;
import com.akif.payment.internal.dto.ReconciliationReport;
import com.akif.payment.internal.dto.StripePayment;
import com.akif.payment.internal.exception.ReconciliationException;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.payment.internal.service.gateway.StripeAmounts;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.param.ChargeListParams;
import com.stripe.param.PaymentIntentRetrieveParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationService {

    static final int AUTHORIZATION_LOOKBACK_DAYS = 8;

    private static final Set<String> MONEY_STATUSES = Set.of("AUTHORIZED", "CAPTURED", "REFUNDED");

    private final Clock clock;

    private final PaymentRepository paymentRepository;
    private final StripeConfig stripeConfig;

    @Value("${payment.reconciliation.max-individual-lookups:200}")
    private int maxIndividualLookups = 200;

    public ReconciliationReport runDailyReconciliation(LocalDate date) {
        LocalDateTime startTimestamp = LocalDateTime.now(clock);

        log.info("[AUDIT] Reconciliation: STARTED | Date: {} | Timestamp: {}", date, startTimestamp);

        try {
            List<Payment> dbPayments = fetchDatabasePayments(date);
            log.info("[AUDIT] Reconciliation: DATABASE_FETCH_COMPLETED | Date: {} | DB Payments Count: {} | Timestamp: {}",
                    date, dbPayments.size(), LocalDateTime.now(clock));

            List<StripePayment> stripePayments = fetchStripePayments(
                    date.minusDays(AUTHORIZATION_LOOKBACK_DAYS), date.plusDays(1));
            long chargesOfTheDay = stripePayments.stream().filter(p -> date.equals(p.createdOn())).count();
            log.info("[AUDIT] Reconciliation: STRIPE_FETCH_COMPLETED | Date: {} | Stripe Payments Count: {} | Created On Date: {} | Timestamp: {}",
                    date, stripePayments.size(), chargesOfTheDay, LocalDateTime.now(clock));

            Set<String> knownIntentIds = knownIntentIds(stripePayments);
            IndividualLookups lookups = lookUpIndividually(dbPayments, stripePayments);
            List<Payment> comparable = dbPayments.stream()
                    .filter(p -> !lookups.unresolved().contains(p.getId()))
                    .toList();
            List<Discrepancy> discrepancies = new ArrayList<>(
                    comparePayments(comparable, lookups.payments(), knownIntentIds, date));
            discrepancies.addAll(lookups.failures());

            ReconciliationReport report = new ReconciliationReport(
                    date,
                    dbPayments.size(),
                    stripePayments.size(),
                    discrepancies,
                    !discrepancies.isEmpty(),
                    LocalDateTime.now(clock)
            );

            if (!discrepancies.isEmpty()) {
                log.warn("[AUDIT] Reconciliation: DISCREPANCIES_FOUND | Date: {} | Total Discrepancies: {} | Timestamp: {}",
                        date, discrepancies.size(), LocalDateTime.now(clock));

                for (Discrepancy discrepancy : discrepancies) {
                    log.warn("[AUDIT] Reconciliation Discrepancy: {} | Payment ID: {} | Stripe Payment Intent ID: {} | DB Amount: {} | Stripe Amount: {} | DB Status: {} | Stripe Status: {} | Description: {} | Timestamp: {}",
                            discrepancy.type(), discrepancy.paymentId(), discrepancy.stripePaymentIntentId(),
                            discrepancy.databaseAmount(), discrepancy.stripeAmount(), discrepancy.databaseStatus(),
                            discrepancy.stripeStatus(), discrepancy.description(), LocalDateTime.now(clock));
                }
            }

            log.info("[AUDIT] Reconciliation: COMPLETED | Date: {} | DB Payments: {} | Stripe Payments: {} | Discrepancies: {} | Has Discrepancies: {} | Duration: {}ms | Timestamp: {}",
                    date, dbPayments.size(), stripePayments.size(), discrepancies.size(), !discrepancies.isEmpty(),
                    java.time.Duration.between(startTimestamp, LocalDateTime.now(clock)).toMillis(), LocalDateTime.now(clock));

            return report;
        } catch (Exception e) {
            log.error("[AUDIT] Reconciliation: FAILED | Date: {} | Error: {} | Timestamp: {}",
                    date, e.getMessage(), LocalDateTime.now(clock));
            throw new ReconciliationException(date, e.getMessage(), e);
        }
    }

    public List<StripePayment> withIndividualLookups(List<Payment> dbPayments, List<StripePayment> stripePayments) {
        return lookUpIndividually(dbPayments, stripePayments).payments();
    }

    public IndividualLookups lookUpIndividually(List<Payment> dbPayments, List<StripePayment> stripePayments) {
        Set<String> listed = new HashSet<>();
        for (StripePayment stripePayment : stripePayments) {
            if (stripePayment.paymentIntentId() != null) {
                listed.add(stripePayment.paymentIntentId());
            }
        }
        List<StripePayment> combined = new ArrayList<>(stripePayments);
        List<Discrepancy> failures = new ArrayList<>();
        Set<Long> unresolved = new HashSet<>();
        int lookups = 0;
        for (Payment dbPayment : dbPayments) {
            String intentId = dbPayment.getStripePaymentIntentId();
            if (intentId == null || listed.contains(intentId) || !MONEY_STATUSES.contains(dbPayment.getStatus().name())) {
                continue;
            }
            if (lookups >= maxIndividualLookups) {
                unresolved.add(dbPayment.getId());
                failures.add(lookupFailed(dbPayment, "Individual lookup skipped: the per-run limit of "
                        + maxIndividualLookups + " lookups was reached"));
                continue;
            }
            lookups++;
            try {
                StripePayment found = fetchIntentCharge(intentId);
                if (found != null) {
                    combined.add(found);
                    listed.add(intentId);
                }
            } catch (ReconciliationException e) {
                unresolved.add(dbPayment.getId());
                failures.add(lookupFailed(dbPayment, "Stripe lookup failed: " + e.getMessage()));
            }
        }
        if (!failures.isEmpty()) {
            log.warn("[AUDIT] Reconciliation: {} payment(s) could not be looked up individually", failures.size());
        }
        return new IndividualLookups(combined, failures, unresolved);
    }

    private Discrepancy lookupFailed(Payment dbPayment, String description) {
        return new Discrepancy(
                DiscrepancyType.LOOKUP_FAILED,
                dbPayment.getId().toString(),
                dbPayment.getStripePaymentIntentId(),
                dbPayment.getAmount(),
                null,
                dbPayment.getStatus().name(),
                null,
                description);
    }

    public record IndividualLookups(List<StripePayment> payments, List<Discrepancy> failures, Set<Long> unresolved) {
    }

    public StripePayment fetchIntentCharge(String intentId) {
        try {
            PaymentIntentRetrieveParams params = PaymentIntentRetrieveParams.builder()
                    .addExpand("latest_charge")
                    .build();
            PaymentIntent intent = PaymentIntent.retrieve(intentId, params, stripeConfig.requestOptions().build());
            Charge charge = intent.getLatestChargeObject();
            return charge == null ? null : toStripePayment(charge);
        } catch (StripeException e) {
            if ("resource_missing".equals(e.getCode())) {
                return null;
            }
            log.error("[AUDIT] Reconciliation: STRIPE_LOOKUP_FAILED | Intent: {} | Stripe Error Code: {}", intentId, e.getCode());
            throw new ReconciliationException(LocalDate.now(clock), "Failed to look up Stripe payment " + intentId, e);
        }
    }

    public List<Payment> fetchDatabasePayments(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        List<Payment> payments = paymentRepository.findWithIntentUpdatedBetween(startOfDay, endOfDay);
        log.debug("Fetched {} payments from database for date: {}", payments.size(), date);

        return payments;
    }

    public List<StripePayment> fetchStripePayments(LocalDate fromInclusive, LocalDate toExclusive) {
        try {
            long startTimestamp = fromInclusive.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            long endTimestamp = toExclusive.atStartOfDay().toEpochSecond(ZoneOffset.UTC);

            ChargeListParams params = ChargeListParams.builder()
                .setCreated(ChargeListParams.Created.builder()
                    .setGte(startTimestamp)
                    .setLt(endTimestamp)
                    .build())
                .setLimit(100L)
                .build();

            List<StripePayment> stripePayments = new ArrayList<>();
            Iterable<Charge> charges = Charge.list(params, stripeConfig.requestOptions().build()).autoPagingIterable();

            for (Charge charge : charges) {
                stripePayments.add(toStripePayment(charge));
            }

            log.debug("Fetched {} payments from Stripe between {} and {}", stripePayments.size(), fromInclusive, toExclusive);
            return stripePayments;

        } catch (StripeException e) {
            log.error("[AUDIT] Reconciliation: STRIPE_FETCH_FAILED | From: {} | To: {} | Stripe Error Code: {} | Error: {} | Timestamp: {}",
                    fromInclusive, toExclusive, e.getCode(), e.getMessage(), LocalDateTime.now(clock));
            throw new ReconciliationException(toExclusive.minusDays(1), "Failed to fetch Stripe payments", e);
        }
    }

    public StripePayment toStripePayment(Charge charge) {
        String currency = charge.getCurrency().toUpperCase(Locale.ROOT);
        LocalDate createdOn = charge.getCreated() == null
                ? null
                : Instant.ofEpochSecond(charge.getCreated()).atZone(ZoneOffset.UTC).toLocalDate();
        return new StripePayment(
            charge.getId(),
            charge.getPaymentIntent(),
            StripeAmounts.fromMinorUnits(charge.getAmount(), currency),
            currency,
            normalizeChargeStatus(charge),
            createdOn
        );
    }

    String normalizeChargeStatus(Charge charge) {
        String status = charge.getStatus() == null ? "" : charge.getStatus().toLowerCase(Locale.ROOT);
        boolean captured = Boolean.TRUE.equals(charge.getCaptured());
        boolean refunded = Boolean.TRUE.equals(charge.getRefunded());
        return switch (status) {
            case "failed" -> "FAILED";
            case "pending" -> "PENDING";
            case "succeeded" -> {
                if (!captured) {
                    yield refunded ? "CANCELLED" : "AUTHORIZED";
                }
                yield refunded ? "REFUNDED" : "CAPTURED";
            }
            default -> status.toUpperCase(Locale.ROOT);
        };
    }

    public List<Discrepancy> comparePayments(List<Payment> dbPayments, List<StripePayment> stripePayments,
                                             Set<String> knownIntentIds, LocalDate date) {
        List<Discrepancy> discrepancies = new ArrayList<>();

        Map<String, StripePayment> stripePaymentsByIntentId = new HashMap<>();
        for (StripePayment stripePayment : stripePayments) {
            if (stripePayment.paymentIntentId() != null) {
                stripePaymentsByIntentId.put(stripePayment.paymentIntentId(), stripePayment);
            }
        }

        for (Payment dbPayment : dbPayments) {
            if (dbPayment.getStripePaymentIntentId() == null) {
                continue;
            }
            StripePayment stripePayment = stripePaymentsByIntentId.get(dbPayment.getStripePaymentIntentId());

            if (stripePayment == null) {
                if (MONEY_STATUSES.contains(dbPayment.getStatus().name())) {
                    discrepancies.add(new Discrepancy(
                        DiscrepancyType.MISSING_IN_STRIPE,
                        dbPayment.getId().toString(),
                        dbPayment.getStripePaymentIntentId(),
                        dbPayment.getAmount(),
                        null,
                        dbPayment.getStatus().name(),
                        null,
                        "Payment exists in database but not found in Stripe"
                    ));
                }
                continue;
            }

            if (dbPayment.getAmount().compareTo(stripePayment.amount()) != 0) {
                discrepancies.add(new Discrepancy(
                    DiscrepancyType.AMOUNT_MISMATCH,
                    dbPayment.getId().toString(),
                    dbPayment.getStripePaymentIntentId(),
                    dbPayment.getAmount(),
                    stripePayment.amount(),
                    dbPayment.getStatus().name(),
                    stripePayment.status(),
                    String.format("Amount mismatch: DB=%s, Stripe=%s",
                        dbPayment.getAmount(), stripePayment.amount())
                ));
            }

            if (!dbPayment.getStatus().name().equals(stripePayment.status())) {
                discrepancies.add(new Discrepancy(
                    DiscrepancyType.STATUS_MISMATCH,
                    dbPayment.getId().toString(),
                    dbPayment.getStripePaymentIntentId(),
                    dbPayment.getAmount(),
                    stripePayment.amount(),
                    dbPayment.getStatus().name(),
                    stripePayment.status(),
                    String.format("Status mismatch: DB=%s, Stripe=%s",
                        dbPayment.getStatus().name(), stripePayment.status())
                ));
            }
        }

        for (StripePayment stripePayment : stripePayments) {
            if (stripePayment.paymentIntentId() == null || !date.equals(stripePayment.createdOn())) {
                continue;
            }
            if (!knownIntentIds.contains(stripePayment.paymentIntentId())) {
                discrepancies.add(new Discrepancy(
                    DiscrepancyType.MISSING_IN_DATABASE,
                    null,
                    stripePayment.paymentIntentId(),
                    null,
                    stripePayment.amount(),
                    null,
                    stripePayment.status(),
                    "Payment exists in Stripe but not found in database"
                ));
            }
        }

        return discrepancies;
    }

    private Set<String> knownIntentIds(List<StripePayment> stripePayments) {
        Set<String> intentIds = new HashSet<>();
        for (StripePayment stripePayment : stripePayments) {
            if (stripePayment.paymentIntentId() != null) {
                intentIds.add(stripePayment.paymentIntentId());
            }
        }
        if (intentIds.isEmpty()) {
            return Set.of();
        }
        Set<String> known = new HashSet<>();
        for (Payment payment : paymentRepository.findByStripePaymentIntentIdIn(intentIds)) {
            known.add(payment.getStripePaymentIntentId());
        }
        return known;
    }
}
