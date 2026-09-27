package com.alekii.alensipay.repository;

import com.alekii.alensipay.domain.Payment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByReference(String reference);

    Optional<Payment> findByProviderReference(String providerReference);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
