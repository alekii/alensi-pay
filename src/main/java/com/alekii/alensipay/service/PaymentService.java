package com.alekii.alensipay.service;

import com.alekii.alensipay.domain.Payment;
import com.alekii.alensipay.domain.PaymentAction;
import com.alekii.alensipay.domain.PaymentStatus;
import com.alekii.alensipay.dto.CreatePaymentRequest;
import com.alekii.alensipay.dto.PaymentResponse;
import com.alekii.alensipay.dto.RefundRequest;
import com.alekii.alensipay.dto.WebhookRequest;
import com.alekii.alensipay.messaging.PaymentMessage;
import com.alekii.alensipay.messaging.PaymentMessageHandler;
import com.alekii.alensipay.messaging.PaymentMessagePublisher;
import com.alekii.alensipay.provider.PaymentProvider;
import com.alekii.alensipay.provider.PaymentProviderRegistry;
import com.alekii.alensipay.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class PaymentService implements PaymentMessageHandler {

    private final PaymentRepository paymentRepository;
    private final PaymentMessagePublisher paymentMessagePublisher;
    private final PaymentProviderRegistry paymentProviderRegistry;
    private final IdempotencyStore idempotencyStore;
    private final AuditService auditService;
    private final PaymentCallbackDispatcher paymentCallbackDispatcher;
    private final int maxRetries;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentMessagePublisher paymentMessagePublisher,
                          PaymentProviderRegistry paymentProviderRegistry,
                          IdempotencyStore idempotencyStore,
                          AuditService auditService,
                          PaymentCallbackDispatcher paymentCallbackDispatcher,
                          @Value("${app.payments.max-retries}") int maxRetries) {
        this.paymentRepository = paymentRepository;
        this.paymentMessagePublisher = paymentMessagePublisher;
        this.paymentProviderRegistry = paymentProviderRegistry;
        this.idempotencyStore = idempotencyStore;
        this.auditService = auditService;
        this.paymentCallbackDispatcher = paymentCallbackDispatcher;
        this.maxRetries = maxRetries;
    }

    public PaymentResponse initiatePayment(String idempotencyKey, CreatePaymentRequest request) {
        return idempotencyStore.find(idempotencyKey)
                .flatMap(paymentRepository::findByReference)
                .map(PaymentResponse::from)
                .orElseGet(() -> createNewPayment(idempotencyKey, request));
    }

    public PaymentResponse getPayment(String reference) {
        return PaymentResponse.from(getPaymentEntity(reference));
    }

    public PaymentResponse refundPayment(String reference, RefundRequest request) {
        Payment payment = getPaymentEntity(reference);
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only completed payments can be refunded");
        }
        payment.setStatus(PaymentStatus.REFUND_PENDING);
        paymentRepository.save(payment);
        auditService.record(reference, "REFUND_REQUESTED", request.reason());
        paymentMessagePublisher.publish(new PaymentMessage(reference, PaymentAction.REFUND, 0));
        return PaymentResponse.from(payment);
    }

    public PaymentResponse handleWebhook(String provider, WebhookRequest request) {
        Payment payment = paymentRepository.findByProviderReference(request.providerReference())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        PaymentProvider paymentProvider = paymentProviderRegistry.getProvider(provider);
        if (!paymentProvider.supports(payment.getProvider())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook provider does not match payment");
        }
        payment.setStatus(paymentProvider.mapStatus(request.status()));
        payment.setFailureReason(request.failureReason());
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            payment.setCompletedAt(OffsetDateTime.now());
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            payment.setRefundedAt(OffsetDateTime.now());
        }
        paymentRepository.save(payment);
        auditService.record(payment.getReference(), "WEBHOOK_RECEIVED", request.status());
        if (payment.getStatus() == PaymentStatus.COMPLETED
                || payment.getStatus() == PaymentStatus.FAILED
                || payment.getStatus() == PaymentStatus.REFUNDED) {
            paymentCallbackDispatcher.dispatch(payment);
        }
        return PaymentResponse.from(payment);
    }

    @Override
    public void handle(PaymentMessage message) {
        Payment payment = getPaymentEntity(message.paymentReference());
        switch (message.action()) {
            case INITIATE -> processInitiation(payment, message.attempt());
            case REFUND -> processRefund(payment, message.attempt());
        }
    }

    private PaymentResponse createNewPayment(String idempotencyKey, CreatePaymentRequest request) {
        Payment payment = Payment.builder()
                .reference(UUID.randomUUID().toString())
                .provider(request.provider().toUpperCase(Locale.ROOT))
                .phoneNumber(request.phoneNumber())
                .amount(request.amount())
                .currency(request.currency().toUpperCase(Locale.ROOT))
                .callbackUrl(request.callbackUrl())
                .status(PaymentStatus.CREATED)
                .idempotencyKey(idempotencyKey)
                .retryCount(0)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        idempotencyStore.remember(idempotencyKey, savedPayment.getReference());
        auditService.record(savedPayment.getReference(), "PAYMENT_CREATED", "Payment request accepted");
        paymentMessagePublisher.publish(new PaymentMessage(savedPayment.getReference(), PaymentAction.INITIATE, 0));
        return PaymentResponse.from(savedPayment);
    }

    private void processInitiation(Payment payment, int attempt) {
        PaymentProvider paymentProvider = paymentProviderRegistry.getProvider(payment.getProvider());
        try {
            ProviderResult providerResult = paymentProvider.initiate(payment);
            payment.setProviderReference(providerResult.providerReference());
            payment.setStatus(providerResult.status());
            payment.setFailureReason(null);
            paymentRepository.save(payment);
            auditService.record(payment.getReference(), "PAYMENT_SUBMITTED", providerResult.providerReference());
        } catch (RuntimeException exception) {
            retryOrFail(payment, PaymentAction.INITIATE, attempt, exception.getMessage());
        }
    }

    private void processRefund(Payment payment, int attempt) {
        PaymentProvider paymentProvider = paymentProviderRegistry.getProvider(payment.getProvider());
        try {
            ProviderResult providerResult = paymentProvider.refund(payment, "Refund requested");
            payment.setStatus(providerResult.status());
            payment.setProviderReference(providerResult.providerReference());
            payment.setFailureReason(null);
            if (providerResult.status() == PaymentStatus.REFUNDED) {
                payment.setRefundedAt(OffsetDateTime.now());
            }
            paymentRepository.save(payment);
            auditService.record(payment.getReference(), "PAYMENT_REFUNDED", providerResult.providerReference());
            paymentCallbackDispatcher.dispatch(payment);
        } catch (RuntimeException exception) {
            retryOrFail(payment, PaymentAction.REFUND, attempt, exception.getMessage());
        }
    }

    private void retryOrFail(Payment payment, PaymentAction action, int attempt, String reason) {
        int nextAttempt = attempt + 1;
        payment.setRetryCount(nextAttempt);
        payment.setFailureReason(reason);
        if (nextAttempt < maxRetries) {
            paymentRepository.save(payment);
            auditService.record(payment.getReference(), "PAYMENT_RETRY_SCHEDULED", reason);
            paymentMessagePublisher.publish(new PaymentMessage(payment.getReference(), action, nextAttempt));
            return;
        }
        payment.setStatus(action == PaymentAction.REFUND ? PaymentStatus.REFUND_FAILED : PaymentStatus.FAILED);
        paymentRepository.save(payment);
        auditService.record(payment.getReference(), "PAYMENT_FAILED", reason);
        paymentCallbackDispatcher.dispatch(payment);
    }

    private Payment getPaymentEntity(String reference) {
        return paymentRepository.findByReference(reference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    }
}
