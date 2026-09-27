package com.alekii.alensipay.controller;

import com.alekii.alensipay.dto.CreatePaymentRequest;
import com.alekii.alensipay.dto.PaymentResponse;
import com.alekii.alensipay.dto.RefundRequest;
import com.alekii.alensipay.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PaymentResponse initiatePayment(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                           @Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.initiatePayment(idempotencyKey, request);
    }

    @GetMapping("/{reference}")
    public PaymentResponse getPayment(@PathVariable String reference) {
        return paymentService.getPayment(reference);
    }

    @PostMapping("/{reference}/refunds")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PaymentResponse refundPayment(@PathVariable String reference, @Valid @RequestBody RefundRequest request) {
        return paymentService.refundPayment(reference, request);
    }
}
