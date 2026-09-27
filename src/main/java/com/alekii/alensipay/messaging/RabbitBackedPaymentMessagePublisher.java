package com.alekii.alensipay.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitBackedPaymentMessagePublisher implements PaymentMessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectProvider<PaymentMessageHandler> paymentMessageHandler;
    private final String queueName;

    public RabbitBackedPaymentMessagePublisher(ObjectProvider<RabbitTemplate> rabbitTemplate,
                                               ObjectProvider<PaymentMessageHandler> paymentMessageHandler,
                                               @Value("${app.messaging.queue}") String queueName) {
        this.rabbitTemplate = rabbitTemplate.getIfAvailable();
        this.paymentMessageHandler = paymentMessageHandler;
        this.queueName = queueName;
    }

    @Override
    public void publish(PaymentMessage message) {
        if (rabbitTemplate != null) {
            try {
                rabbitTemplate.convertAndSend(queueName, message);
                return;
            } catch (RuntimeException ignored) {
            }
        }
        paymentMessageHandler.getObject().handle(message);
    }
}
