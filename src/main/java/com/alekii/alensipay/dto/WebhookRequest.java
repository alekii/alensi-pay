package com.alekii.alensipay.dto;

import jakarta.validation.constraints.NotBlank;

public record WebhookRequest(
        @NotBlank String providerReference,
        @NotBlank String status,
        String failureReason
) {
}
