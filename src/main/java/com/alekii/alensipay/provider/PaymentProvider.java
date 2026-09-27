package com.alekii.alensipay.provider;

import com.alekii.alensipay.domain.Payment;
import com.alekii.alensipay.domain.PaymentStatus;
import com.alekii.alensipay.service.ProviderResult;

public interface PaymentProvider {

    boolean supports(String provider);

    ProviderResult initiate(Payment payment);

    ProviderResult refund(Payment payment, String reason);

    PaymentStatus mapStatus(String providerStatus);
}
