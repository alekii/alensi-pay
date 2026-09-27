package com.alekii.alensipay.messaging;

import com.alekii.alensipay.domain.PaymentAction;

public record PaymentMessage(String paymentReference, PaymentAction action, int attempt) {
}
