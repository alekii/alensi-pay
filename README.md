# alensi-pay

Payment Processing Platform

Minimal Spring Boot payment service demonstrating:

- payment initiation
- M-Pesa / mobile-money provider abstraction
- provider webhooks and merchant callbacks
- transaction lifecycle and payment status tracking
- idempotency
- retries and failure handling
- refunds
- API-key authentication
- audit logging
- PostgreSQL, Redis, RabbitMQ, and Docker based runtime stack

## Endpoints

- `POST /api/payments` with `X-API-Key` and `Idempotency-Key`
- `GET /api/payments/{reference}`
- `POST /api/payments/{reference}/refunds` with `X-API-Key`
- `POST /api/webhooks/mpesa`

## Local stack

```bash
docker compose up --build
```

Default application API key:

```text
change-me
```
