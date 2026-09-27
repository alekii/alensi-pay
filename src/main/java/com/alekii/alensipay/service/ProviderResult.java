package com.alekii.alensipay.service;

import com.alekii.alensipay.domain.PaymentStatus;

public record ProviderResult(String providerReference, PaymentStatus status) {
}
