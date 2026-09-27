package com.alekii.alensipay.messaging;

public interface PaymentMessagePublisher {

    void publish(PaymentMessage message);
}
