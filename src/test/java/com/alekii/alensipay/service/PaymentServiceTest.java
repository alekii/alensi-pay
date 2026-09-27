package com.alekii.alensipay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alekii.alensipay.domain.Payment;
import com.alekii.alensipay.domain.PaymentAction;
import com.alekii.alensipay.domain.PaymentStatus;
import com.alekii.alensipay.dto.CreatePaymentRequest;
import com.alekii.alensipay.dto.RefundRequest;
import com.alekii.alensipay.dto.WebhookRequest;
import com.alekii.alensipay.messaging.PaymentMessage;
import com.alekii.alensipay.messaging.PaymentMessagePublisher;
import com.alekii.alensipay.provider.PaymentProvider;
import com.alekii.alensipay.provider.PaymentProviderRegistry;
import com.alekii.alensipay.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMessagePublisher paymentMessagePublisher;

    @Mock
    private PaymentProviderRegistry paymentProviderRegistry;

    @Mock
    private IdempotencyStore idempotencyStore;

    @Mock
    private AuditService auditService;

    @Mock
    private PaymentCallbackDispatcher paymentCallbackDispatcher;

    @Mock
    private PaymentProvider paymentProvider;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                paymentMessagePublisher,
                paymentProviderRegistry,
                idempotencyStore,
                auditService,
                paymentCallbackDispatcher,
                3
        );
    }

    @Test
    void initiatePaymentStoresIdempotencyAndPublishesProcessingMessage() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                "mpesa",
                "254712345678",
                BigDecimal.valueOf(100),
                "KES",
                "https://merchant.example/callback"
        );
        when(idempotencyStore.find("idem-1")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(UUID.randomUUID());
            return payment;
        });

        var response = paymentService.initiatePayment("idem-1", request);

        assertThat(response.status()).isEqualTo(PaymentStatus.CREATED);
        verify(idempotencyStore).remember("idem-1", response.reference());
        verify(paymentMessagePublisher).publish(new PaymentMessage(response.reference(), PaymentAction.INITIATE, 0));
    }

    @Test
    void duplicateIdempotencyKeyReturnsExistingPayment() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .reference("pay-123")
                .provider("MPESA")
                .phoneNumber("254712345678")
                .amount(BigDecimal.valueOf(100))
                .currency("KES")
                .status(PaymentStatus.PENDING)
                .retryCount(0)
                .build();
        when(idempotencyStore.find("idem-1")).thenReturn(Optional.of("pay-123"));
        when(paymentRepository.findByReference("pay-123")).thenReturn(Optional.of(payment));

        var response = paymentService.initiatePayment("idem-1", new CreatePaymentRequest(
                "mpesa", "254712345678", BigDecimal.valueOf(100), "KES", null
        ));

        assertThat(response.reference()).isEqualTo("pay-123");
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(paymentMessagePublisher, never()).publish(any());
    }

    @Test
    void processingFailureRetriesAndEventuallyMarksPaymentFailed() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .reference("pay-1")
                .provider("MPESA")
                .phoneNumber("254712345678")
                .amount(BigDecimal.valueOf(100))
                .currency("KES")
                .status(PaymentStatus.CREATED)
                .retryCount(0)
                .build();
        when(paymentRepository.findByReference("pay-1")).thenReturn(Optional.of(payment));
        when(paymentProviderRegistry.getProvider("MPESA")).thenReturn(paymentProvider);
        when(paymentProvider.initiate(payment)).thenThrow(new IllegalStateException("provider unavailable"));

        paymentService.handle(new PaymentMessage("pay-1", PaymentAction.INITIATE, 0));
        paymentService.handle(new PaymentMessage("pay-1", PaymentAction.INITIATE, 1));
        paymentService.handle(new PaymentMessage("pay-1", PaymentAction.INITIATE, 2));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getRetryCount()).isEqualTo(3);
        verify(paymentMessagePublisher).publish(new PaymentMessage("pay-1", PaymentAction.INITIATE, 1));
        verify(paymentMessagePublisher).publish(new PaymentMessage("pay-1", PaymentAction.INITIATE, 2));
        verify(paymentCallbackDispatcher).dispatch(payment);
    }

    @Test
    void webhookUpdatesSuccessfulPaymentAndDispatchesCallback() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .reference("pay-1")
                .provider("MPESA")
                .providerReference("MPESA-pay-1")
                .phoneNumber("254712345678")
                .amount(BigDecimal.valueOf(100))
                .currency("KES")
                .status(PaymentStatus.PENDING)
                .retryCount(0)
                .build();
        when(paymentRepository.findByProviderReference("MPESA-pay-1")).thenReturn(Optional.of(payment));
        when(paymentProviderRegistry.getProvider("mpesa")).thenReturn(paymentProvider);
        when(paymentProvider.supports("MPESA")).thenReturn(true);
        when(paymentProvider.mapStatus("COMPLETED")).thenReturn(PaymentStatus.COMPLETED);

        var response = paymentService.handleWebhook("mpesa", new WebhookRequest("MPESA-pay-1", "COMPLETED", null));

        assertThat(response.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getCompletedAt()).isNotNull();
        verify(paymentCallbackDispatcher).dispatch(payment);
    }

    @Test
    void webhookRejectsProviderMismatch() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .reference("pay-1")
                .provider("AIRTEL")
                .providerReference("MPESA-pay-1")
                .phoneNumber("254712345678")
                .amount(BigDecimal.valueOf(100))
                .currency("KES")
                .status(PaymentStatus.PENDING)
                .retryCount(0)
                .build();
        when(paymentRepository.findByProviderReference("MPESA-pay-1")).thenReturn(Optional.of(payment));
        when(paymentProviderRegistry.getProvider("mpesa")).thenReturn(paymentProvider);
        when(paymentProvider.supports("AIRTEL")).thenReturn(false);

        assertThrows(ResponseStatusException.class,
                () -> paymentService.handleWebhook("mpesa", new WebhookRequest("MPESA-pay-1", "COMPLETED", null)));
        verify(paymentCallbackDispatcher, never()).dispatch(any());
    }

    @Test
    void duplicateTerminalWebhookDoesNotRewriteTimestampOrRedispatchCallback() {
        OffsetDateTime completedAt = OffsetDateTime.now().minusMinutes(5);
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .reference("pay-1")
                .provider("MPESA")
                .providerReference("MPESA-pay-1")
                .phoneNumber("254712345678")
                .amount(BigDecimal.valueOf(100))
                .currency("KES")
                .status(PaymentStatus.COMPLETED)
                .retryCount(0)
                .completedAt(completedAt)
                .build();
        when(paymentRepository.findByProviderReference("MPESA-pay-1")).thenReturn(Optional.of(payment));
        when(paymentProviderRegistry.getProvider("mpesa")).thenReturn(paymentProvider);
        when(paymentProvider.supports("MPESA")).thenReturn(true);
        when(paymentProvider.mapStatus("COMPLETED")).thenReturn(PaymentStatus.COMPLETED);

        var response = paymentService.handleWebhook("mpesa", new WebhookRequest("MPESA-pay-1", "COMPLETED", null));

        assertThat(response.completedAt()).isEqualTo(completedAt);
        verify(paymentCallbackDispatcher, never()).dispatch(payment);
    }

    @Test
    void refundRequiresCompletedPayment() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .reference("pay-1")
                .provider("MPESA")
                .phoneNumber("254712345678")
                .amount(BigDecimal.valueOf(100))
                .currency("KES")
                .status(PaymentStatus.PENDING)
                .retryCount(0)
                .build();
        when(paymentRepository.findByReference("pay-1")).thenReturn(Optional.of(payment));

        assertThrows(ResponseStatusException.class,
                () -> paymentService.refundPayment("pay-1", new RefundRequest("customer request")));
    }

    @Test
    void completedPaymentRefundTransitionsToRefundPendingAndPublishesMessage() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .reference("pay-1")
                .provider("MPESA")
                .providerReference("MPESA-pay-1")
                .phoneNumber("254712345678")
                .amount(BigDecimal.valueOf(100))
                .currency("KES")
                .status(PaymentStatus.COMPLETED)
                .retryCount(0)
                .completedAt(OffsetDateTime.now())
                .build();
        when(paymentRepository.findByReference("pay-1")).thenReturn(Optional.of(payment));

        var response = paymentService.refundPayment("pay-1", new RefundRequest("customer request"));

        assertThat(response.status()).isEqualTo(PaymentStatus.REFUND_PENDING);
        verify(paymentRepository).save(payment);
        verify(auditService).record("pay-1", "REFUND_REQUESTED", "customer request");
        verify(paymentMessagePublisher).publish(new PaymentMessage("pay-1", PaymentAction.REFUND, 0));
    }
}
