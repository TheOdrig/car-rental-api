package com.akif.payment.internal.service;

import com.akif.payment.api.PaymentNotFoundException;
import com.akif.payment.domain.Payment;
import com.akif.payment.internal.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PaymentSupport {

    private final PaymentRepository paymentRepository;
    private final PlatformTransactionManager transactionManager;

    TransactionTemplate isolated() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    Payment requirePayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
    }

    Payment requirePaymentForUpdate(Long paymentId) {
        return paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
    }
}
