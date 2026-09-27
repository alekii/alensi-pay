# Alensi Pay

### Global Payment Orchestration Platform

Alensi Pay is a payment orchestration platform designed to provide a unified API for processing payments across multiple providers, payment methods, currencies, and regions.

The platform abstracts provider-specific complexity behind a consistent payment API, allowing merchants and applications to integrate with multiple payment providers without coupling their systems to individual provider implementations.

The project explores the architecture and engineering challenges involved in building reliable, extensible, and observable payment infrastructure for global markets.

---

## Overview

Payment providers differ significantly in their APIs, capabilities, transaction states, authentication mechanisms, failure modes, and settlement processes.

Alensi Pay provides a unified abstraction layer between applications and payment providers.

```text
                    Merchant / Application
                            |
                            v
                    Unified Payment API
                            |
                            v
                    Payment Orchestration
                            |
                +-----------+-----------+
                |                       |
          Routing Engine          Payment Service
                |                       |
        +-------+-------+               |
        |       |       |               |
        v       v       v               v
    Provider  Provider Provider      Transaction
       A         B       C            Lifecycle
```

The goal is to make adding or replacing payment providers an implementation detail rather than a fundamental change to the merchant's integration.

## Key Capabilities

### Payment Processing

- Payment initiation
- Authorization
- Capture
- Refunds
- Payment status tracking
- Transaction lifecycle management
- Idempotency
- Retry and failure handling
- Webhooks and callbacks
- Audit logging

### Multi-Provider Architecture

Provider-specific implementations are isolated behind a common payment provider abstraction.

This allows the platform to support different providers without exposing provider-specific APIs and behavior to merchants.

```text
                    Payment API
                        |
                Payment Service
                        |
                Routing Engine
                        |
          +-------------+-------------+
          |             |             |
       Adapter A     Adapter B     Adapter C
          |             |             |
       Provider A    Provider B    Provider C
```

Adding a new provider should require implementing a provider adapter rather than modifying the core payment domain.

## Global Payments

Alensi Pay is designed around the requirements of international payment processing.

### Regions

- Africa
- Europe
- North America
- Asia

### Payment Methods

The architecture is designed to accommodate:

- Cards
- Mobile money
- Bank transfers
- Digital wallets
- Regional payment methods
- Alternative payment methods

### Currencies

The platform supports a multi-currency model covering:

- Transaction currency
- Settlement currency
- Currency conversion
- FX rates
- Provider-supported currencies

Provider and payment-method capabilities are treated as configurable rather than hardcoded.

## Intelligent Payment Routing

One of the core capabilities of Alensi Pay is intelligent provider routing.

Instead of forcing every payment through a single provider, the routing engine can evaluate multiple factors when selecting a provider.

```text
                    Payment Request
                           |
                           v
                   Routing Engine
                           |
        +------------------+------------------+
        |                  |                  |
    Currency           Geography          Payment Method
        |                  |                  |
    Provider          Provider            Provider
    Capability        Availability        Capability
        |                  |                  |
        +------------------+------------------+
                           |
                           v
                  Provider Selection
                           |
                           v
                    Payment Provider
```

Potential routing signals include:

- Country
- Currency
- Payment method
- Provider availability
- Provider capabilities
- Transaction amount
- Provider fees
- Historical success rate
- Latency
- Merchant configuration
- Provider health

This creates the foundation for intelligent routing and payment optimization.

## Payment Lifecycle

A typical payment flow:

```text
                    Create Payment
                          |
                          v
                     Validate
                          |
                          v
                    Idempotency
                       Check
                          |
                          v
                  Routing Decision
                          |
                          v
                  Provider Adapter
                          |
                          v
                 Payment Provider
                          |
                          v
              +-----------+-----------+
              |                       |
           Success                  Failure
              |                       |
              v                       v
       Update Payment             Retry / Fail
              |
              v
          Webhook
              |
              v
      Final Transaction State
```

## Reliability

Payment systems must remain reliable even when external providers are not.

Alensi Pay explores:

- Idempotent payment operations
- Safe retries
- Provider timeouts
- Circuit breakers
- Duplicate webhook protection
- Transaction state consistency
- Provider failover
- Dead-letter queues
- Asynchronous processing
- Event-driven transaction updates

The system should never assume that an external provider is always available or that a request will produce a response.

## Observability

Payment infrastructure requires visibility into both application behavior and provider performance.

Alensi Pay is designed to expose:

- Payment success rates
- Provider success rates
- Payment latency
- Provider latency
- Failed transactions
- Pending transactions
- Retry counts
- Webhook failures
- Provider availability

Observability capabilities are designed to integrate with Alensi Observe.

## Security

Payment operations require strict handling of sensitive data and credentials.

The platform is designed around:

- API authentication
- Authorization
- Secure credential management
- Provider credential isolation
- Request validation
- Webhook signature validation
- Idempotency protection
- Audit trails
- Sensitive-data minimization

Production credentials and secrets must never be committed to the repository.

## Technology Stack

### Backend

- Java
- Spring Boot
- REST APIs

### Data

- PostgreSQL
- Redis

### Messaging

- RabbitMQ
- Event-driven processing

### Infrastructure

- Docker
- Kubernetes
- AWS

### Observability

- OpenTelemetry
- Prometheus
- Grafana

## Architecture

The platform is designed around clear separation between the payment domain and provider-specific integrations.

```text
                         Alensi Pay
                              |
                 +------------+------------+
                 |                         |
           Payment Domain            Provider Layer
                 |                         |
        +--------+--------+       +-------+-------+
        |        |        |       |       |       |
     Payment   Refund   Routing  Card   Mobile   Bank
     Service   Service  Engine  APIs   Money     APIs
                 |                         |
                 +------------+------------+
                              |
                       Event / Messaging
                              |
                 +------------+------------+
                 |                         |
             PostgreSQL                Redis
                 |
          Transaction Data
```

Detailed architecture diagrams, ERD diagrams, sequence diagrams, and architecture decisions are documented under `/docs`.

## Engineering Principles

- Provider-agnostic payment domain
- Explicit transaction state management
- Idempotent operations
- Event-driven processing
- Failure-aware design
- Observable systems
- Secure-by-design integrations
- Extensible provider architecture
- Configuration over hardcoding
- Auditable financial operations

## Project Status

Alensi Pay is being developed as a reference implementation exploring the architecture and engineering challenges of modern global payment infrastructure.

The project will progressively introduce:

- Payment provider adapters
- Multi-currency support
- Intelligent routing
- Transaction orchestration
- Webhook processing
- Failure recovery
- Provider health monitoring
- Observability
- Automated testing
- Documentation

## Documentation

Architecture and design documentation:

- System Architecture
- Entity Relationship Diagram
- API Documentation
- Payment Flows
- Architecture Decision Records

## Getting Started

### Prerequisites

- Java 21+
- Docker
- Docker Compose
- PostgreSQL
- Redis
- RabbitMQ

### Clone

```bash
git clone https://github.com/alekii/alensi-pay.git
cd alensi-pay
```

### Start Infrastructure

```bash
docker compose up -d
```

### Run the Application

```bash
./mvnw spring-boot:run
```

Configuration should be supplied through environment variables or local configuration.

Never commit production credentials or provider secrets to the repository.

## Testing

The project includes automated tests covering:

- Payment lifecycle
- Provider adapters
- Idempotency
- Webhook processing
- Routing decisions
- Failure scenarios
- Transaction state transitions

Additional integration and end-to-end tests will be added as the platform evolves.

## License

This project is currently intended as a portfolio and engineering reference implementation.
