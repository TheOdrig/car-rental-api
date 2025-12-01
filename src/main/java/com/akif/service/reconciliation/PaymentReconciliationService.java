package com.akif.service.reconciliation;

import com.akif.dto.reconciliation.Discrepancy;
import com.akif.dto.reconciliation.ReconciliationReport;
import com.akif.dto.reconciliation.StripePayment;
import com.akif.enums.DiscrepancyType;
import com.akif.exception.ReconciliationException;
import com.akif.model.Payment;
import com.akif.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.param.ChargeListParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationService {

    private final PaymentRepository paymentRepository;

    public ReconciliationReport runDailyReconciliation(LocalDate date) {
        log.info("Starting reconciliation for date: {}", date);
        
        try {
            List<Payment> dbPayments = fetchDatabasePayments(date);
            List<StripePayment> stripePayments = fetchStripePayments(date);
            List<Discrepancy> discrepancies = comparePayments(dbPayments, stripePayments);
            
            ReconciliationReport report = new ReconciliationReport(
                date,
                dbPayments.size(),
                stripePayments.size(),
                discrepancies,
                !discrepancies.isEmpty(),
                LocalDateTime.now()
            );
            
            log.info("Reconciliation completed. DB payments: {}, Stripe payments: {}, Discrepancies: {}", 
                dbPayments.size(), stripePayments.size(), discrepancies.size());
            
            return report;
        } catch (Exception e) {
            log.error("Reconciliation failed for date: {}", date, e);
            throw new ReconciliationException(date, e.getMessage(), e);
        }
    }

    public List<Payment> fetchDatabasePayments(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        
        List<Payment> payments = paymentRepository.findByCreateTimeBetween(startOfDay, endOfDay);
        log.debug("Fetched {} payments from database for date: {}", payments.size(), date);
        
        return payments;
    }

    public List<StripePayment> fetchStripePayments(LocalDate date) {
        try {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            
            long startTimestamp = startOfDay.toEpochSecond(ZoneOffset.UTC);
            long endTimestamp = endOfDay.toEpochSecond(ZoneOffset.UTC);
            
            ChargeListParams params = ChargeListParams.builder()
                .setCreated(ChargeListParams.Created.builder()
                    .setGte(startTimestamp)
                    .setLt(endTimestamp)
                    .build())
                .setLimit(100L)
                .build();
            
            List<StripePayment> stripePayments = new ArrayList<>();
            Iterable<Charge> charges = Charge.list(params).autoPagingIterable();
            
            for (Charge charge : charges) {
                stripePayments.add(new StripePayment(
                    charge.getId(),
                    charge.getPaymentIntent(),
                    BigDecimal.valueOf(charge.getAmount()).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP),
                    charge.getCurrency().toUpperCase(),
                    charge.getStatus()
                ));
            }
            
            log.debug("Fetched {} payments from Stripe for date: {}", stripePayments.size(), date);
            return stripePayments;
            
        } catch (StripeException e) {
            log.error("Failed to fetch Stripe payments for date: {}", date, e);
            throw new ReconciliationException(date, "Failed to fetch Stripe payments", e);
        }
    }

    public List<Discrepancy> comparePayments(List<Payment> dbPayments, List<StripePayment> stripePayments) {
        List<Discrepancy> discrepancies = new ArrayList<>();

        Map<String, Payment> dbPaymentsByIntentId = new HashMap<>();
        Map<String, StripePayment> stripePaymentsByIntentId = new HashMap<>();

        for (Payment payment : dbPayments) {
            if (payment.getStripePaymentIntentId() != null) {
                dbPaymentsByIntentId.put(payment.getStripePaymentIntentId(), payment);
            }
        }

        for (StripePayment stripePayment : stripePayments) {
            if (stripePayment.paymentIntentId() != null) {
                stripePaymentsByIntentId.put(stripePayment.paymentIntentId(), stripePayment);
            }
        }

        for (Payment dbPayment : dbPayments) {
            if (dbPayment.getStripePaymentIntentId() != null) {
                StripePayment stripePayment = stripePaymentsByIntentId.get(dbPayment.getStripePaymentIntentId());
                
                if (stripePayment == null) {
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
                } else {
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

                    String normalizedDbStatus = normalizeStatus(dbPayment.getStatus().name());
                    String normalizedStripeStatus = normalizeStatus(stripePayment.status());
                    
                    if (!normalizedDbStatus.equals(normalizedStripeStatus)) {
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
            }
        }

        for (StripePayment stripePayment : stripePayments) {
            if (stripePayment.paymentIntentId() != null) {
                Payment dbPayment = dbPaymentsByIntentId.get(stripePayment.paymentIntentId());
                
                if (dbPayment == null) {
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
        }
        
        return discrepancies;
    }

    private String normalizeStatus(String status) {
        if (status == null) return "UNKNOWN";
        
        String upper = status.toUpperCase();

        return switch (upper) {
            case "SUCCEEDED", "CAPTURED" -> "CAPTURED";
            case "PENDING", "PROCESSING" -> "PENDING";
            case "FAILED", "CANCELED" -> "FAILED";
            case "REFUNDED" -> "REFUNDED";
            default -> upper;
        };
    }
}
