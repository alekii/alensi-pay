# Alensi Pay

A standalone payment processing platform focused on securely moving money.

## Overview

Alensi Pay provides the core building blocks needed to initiate, process, track, and reconcile payments across multiple providers. It is designed with reliability, observability, and extensibility in mind.

## Features

- Payment initiation
- M-Pesa and mobile-money integration
- Payment provider abstraction
- Webhooks and callbacks
- Transaction lifecycle management
- Idempotency protection
- Retry and failure handling
- Refund processing
- Payment status tracking
- API authentication
- Audit logging

## Technology Stack

- Java
- Spring Boot
- PostgreSQL
- Redis
- RabbitMQ
- Docker

## Getting Started

### Prerequisites

Make sure the following tools are installed:

- Java
- Docker and Docker Compose
- PostgreSQL, Redis, and RabbitMQ, if running services locally

### Installation

Clone the repository and move into the project directory:

```bash
git clone https://github.com/alekii/alensi-pay.git
cd alensi-pay
```

Start the supporting services with Docker Compose, if available:

```bash
docker compose up -d
```

Then start the Spring Boot application using your preferred build tool.

## Configuration

Configure environment-specific values such as database credentials, Redis and RabbitMQ connection details, API authentication settings, and payment-provider credentials through environment variables or your application configuration files.

Never commit production secrets or private credentials to the repository.

## Payment Flow

A typical payment flow consists of the following stages:

1. Authenticate the API request.
2. Create a payment using an idempotency key.
3. Submit the payment to the selected provider.
4. Receive and validate provider callbacks or webhooks.
5. Update the transaction status.
6. Record relevant audit events.
7. Process refunds when required.

## Reliability and Security

Because payment systems handle sensitive financial operations, implementations should:

- Validate all incoming requests and provider callbacks.
- Enforce idempotency for payment-creation requests.
- Protect credentials and sensitive configuration.
- Use retries carefully to avoid duplicate charges.
- Record an auditable transaction history.
- Monitor failed, pending, and disputed transactions.

## Development

When contributing changes:

- Add tests for new payment flows and edge cases.
- Keep provider-specific logic behind the provider abstraction.
- Preserve transaction state consistency.
- Document new configuration values and API behavior.

## Contributing

Contributions are welcome. Please open an issue to discuss significant changes before submitting a pull request.

## License

No license has been specified yet. Add a `LICENSE` file before distributing this project publicly.
