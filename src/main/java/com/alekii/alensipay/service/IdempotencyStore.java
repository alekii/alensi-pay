package com.alekii.alensipay.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyStore {

    private final StringRedisTemplate stringRedisTemplate;
    private final Duration ttl;
    private final Map<String, LocalEntry> fallbackCache = new ConcurrentHashMap<>();

    public IdempotencyStore(ObjectProvider<StringRedisTemplate> stringRedisTemplate,
                            @Value("${app.idempotency.ttl}") Duration ttl) {
        this.stringRedisTemplate = stringRedisTemplate.getIfAvailable();
        this.ttl = ttl;
    }

    public Optional<String> find(String key) {
        if (stringRedisTemplate != null) {
            try {
                return Optional.ofNullable(stringRedisTemplate.opsForValue().get(key));
            } catch (RuntimeException ignored) {
            }
        }
        LocalEntry localEntry = fallbackCache.get(key);
        if (localEntry == null) {
            return Optional.empty();
        }
        if (localEntry.expiresAt().isBefore(OffsetDateTime.now())) {
            fallbackCache.remove(key);
            return Optional.empty();
        }
        return Optional.of(localEntry.paymentReference());
    }

    public void remember(String key, String paymentReference) {
        OffsetDateTime expiresAt = OffsetDateTime.now().plus(ttl);
        if (stringRedisTemplate != null) {
            try {
                stringRedisTemplate.opsForValue().set(key, paymentReference, ttl);
            } catch (RuntimeException ignored) {
            }
        }
        fallbackCache.putIfAbsent(key, new LocalEntry(paymentReference, expiresAt));
    }

    private record LocalEntry(String paymentReference, OffsetDateTime expiresAt) {
    }
}
