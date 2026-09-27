package com.alekii.alensipay.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentMessageListener {

    private final PaymentMessageHandler paymentMessageHandler;

    public PaymentMessageListener(PaymentMessageHandler paymentMessageHandler) {
        this.paymentMessageHandler = paymentMessageHandler;
    }

    @RabbitListener(queues = "${app.messaging.queue}")
    public void onMessage(PaymentMessage message) {
        paymentMessageHandler.handle(message);
    }
}
