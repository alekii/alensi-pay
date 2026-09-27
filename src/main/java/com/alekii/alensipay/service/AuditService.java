package com.alekii.alensipay.service;

import com.alekii.alensipay.domain.AuditLog;
import com.alekii.alensipay.repository.AuditLogRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(String paymentReference, String action, String details) {
        auditLogRepository.save(AuditLog.builder()
                .paymentReference(paymentReference)
                .action(action)
                .details(details)
                .createdAt(OffsetDateTime.now())
                .build());
    }
}
