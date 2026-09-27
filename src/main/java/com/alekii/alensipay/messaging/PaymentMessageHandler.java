package com.alekii.alensipay.messaging;

public interface PaymentMessageHandler {

    void handle(PaymentMessage message);
}
