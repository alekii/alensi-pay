package com.alekii.alensipay.controller;

import com.alekii.alensipay.dto.PaymentResponse;
import com.alekii.alensipay.dto.WebhookRequest;
import com.alekii.alensipay.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final PaymentService paymentService;

    public WebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{provider}")
    public PaymentResponse handleWebhook(@PathVariable String provider, @Valid @RequestBody WebhookRequest request) {
        return paymentService.handleWebhook(provider, request);
    }
}
