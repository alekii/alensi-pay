package com.alekii.alensipay.dto;

import com.alekii.alensipay.domain.Payment;
import com.alekii.alensipay.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentResponse(
        String reference,
        String provider,
        String phoneNumber,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String providerReference,
        int retryCount,
        String failureReason,
        String callbackUrl,
        OffsetDateTime completedAt,
        OffsetDateTime refundedAt
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getReference(),
                payment.getProvider(),
                payment.getPhoneNumber(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getProviderReference(),
                payment.getRetryCount(),
                payment.getFailureReason(),
                payment.getCallbackUrl(),
                payment.getCompletedAt(),
                payment.getRefundedAt()
        );
    }
}
