package com.alekii.alensipay.provider;

import com.alekii.alensipay.domain.Payment;
import com.alekii.alensipay.domain.PaymentStatus;
import com.alekii.alensipay.service.ProviderResult;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class MpesaPaymentProvider implements PaymentProvider {

    @Override
    public boolean supports(String provider) {
        return "MPESA".equalsIgnoreCase(provider) || "MOBILE_MONEY_MPESA".equalsIgnoreCase(provider);
    }

    @Override
    public ProviderResult initiate(Payment payment) {
        if (!payment.getPhoneNumber().matches("^254\\d{9}$")) {
            throw new IllegalArgumentException("M-Pesa payments require a Kenyan MSISDN in 254XXXXXXXXX format");
        }
        return new ProviderResult("MPESA-" + payment.getReference(), PaymentStatus.PENDING);
    }

    @Override
    public ProviderResult refund(Payment payment, String reason) {
        return new ProviderResult(payment.getProviderReference() + "-REFUND", PaymentStatus.REFUNDED);
    }

    @Override
    public PaymentStatus mapStatus(String providerStatus) {
        return switch (providerStatus.toUpperCase(Locale.ROOT)) {
            case "SUCCESS", "COMPLETED" -> PaymentStatus.COMPLETED;
            case "FAILED" -> PaymentStatus.FAILED;
            case "REFUNDED" -> PaymentStatus.REFUNDED;
            default -> PaymentStatus.PENDING;
        };
    }
}
