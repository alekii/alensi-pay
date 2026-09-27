package com.alekii.alensipay.service;

import com.alekii.alensipay.domain.Payment;
import com.alekii.alensipay.dto.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class HttpPaymentCallbackDispatcher implements PaymentCallbackDispatcher {

    private static final Logger log = LoggerFactory.getLogger(HttpPaymentCallbackDispatcher.class);

    private final RestTemplate restTemplate;

    public HttpPaymentCallbackDispatcher(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void dispatch(Payment payment) {
        if (payment.getCallbackUrl() == null || payment.getCallbackUrl().isBlank()) {
            return;
        }
        try {
            restTemplate.postForEntity(payment.getCallbackUrl(), new HttpEntity<>(PaymentResponse.from(payment)), Void.class);
        } catch (RuntimeException exception) {
            log.warn("Failed to send callback for payment {}", payment.getReference(), exception);
        }
    }
}
