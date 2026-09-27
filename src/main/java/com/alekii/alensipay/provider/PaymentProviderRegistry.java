package com.alekii.alensipay.provider;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PaymentProviderRegistry {

    private final List<PaymentProvider> paymentProviders;

    public PaymentProviderRegistry(List<PaymentProvider> paymentProviders) {
        this.paymentProviders = paymentProviders;
    }

    public PaymentProvider getProvider(String provider) {
        return paymentProviders.stream()
                .filter(candidate -> candidate.supports(provider))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported provider: " + provider));
    }
}
