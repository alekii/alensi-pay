package com.alekii.alensipay.service;

import com.alekii.alensipay.domain.Payment;

public interface PaymentCallbackDispatcher {

    void dispatch(Payment payment);
}
