# Wallet Transaction Service

Production-style wallet service built with Java 25, Spring Boot 4, Spring
Modulith, PostgreSQL, Redis, JWT authentication, and event-driven domain
flows.

## Overview

The service supports:

- user registration, login, refresh, and logout
- wallet creation and balance lookup
- funding, withdrawal, transfer, reversal, and transaction history
- idempotent write operations through AOP
- event publication through Spring application events by default, with
  switchable publisher infrastructure

Base URL: `http://localhost:8080/api/v1`

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Setup

### Prerequisites

- Docker Desktop or Docker Engine with Compose support
- Internet access on the first build so Docker can pull base images and Maven can resolve dependencies inside the build stage

### Official Run Path

Run the full stack with Docker Compose:

```bash
docker compose up --build
```

This starts:

- `app` on port `8080`
- `postgres` on port `5432`
- `redis` on port `6379`

After startup:

- API base URL: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

To stop the stack:

```bash
docker compose down
```

## Architecture Decisions

### Event-driven design

Significant state changes publish domain events. Side effects live in
listeners instead of inline business logic.

Default broker:

- Spring `ApplicationEventPublisher`

Switchable publisher:

- `spring` via `SpringEventPublisher`
- `kafka` via `KafkaEventPublisher`

Property:

- `APP_EVENTS_BROKER=spring`

### Spring Modulith boundaries

Modules are kept explicit:

- `common`
- `auth`
- `wallet`
- `transaction`
- `notification`

Cross-module access happens through exposed named interfaces such as:

- `auth::api`
- `wallet::api`
- `wallet::events`
- `transaction::events`

JPA entities are not shared across modules. Cross-module persistence
references are stored as UUID IDs.

### Locking strategy

Single-wallet balance updates use optimistic locking through `@Version` on
`Wallet`.

Transfers use pessimistic locking through `findByIdWithLock(...)` and
always lock in deterministic UUID order. This is why transfer code
explicitly sorts wallet IDs before fetching both locks.

### Idempotency

Funding, withdrawal, and transfer methods are annotated with `@Idempotent`.

The aspect:

1. reads `Idempotency-Key`
2. checks Redis for a cached response
3. returns the cached response on a hit
4. executes once and stores the serialized API response for 24 hours on a
   miss

## Assumptions

These assumptions were implemented because the original spec leaves them
implicit:

- wallet stores `userId`, `balance`, `currency`, `status`, timestamps, and
  optimistic lock version
- one wallet per user per currency is enforced
- currency is stored as a string such as `NGN`
- no FX conversion logic is in scope
- transaction history supports filtering by date range, type, status, and
  amount range
- JWT logout deletes refresh tokens only; access tokens remain valid until
  expiry

## JWT Tradeoff

Logout invalidates the refresh token record, but issued access tokens
remain valid until their expiry. A production-grade next step would be a
Redis-backed access-token blacklist.

## Environment Variables

The Docker Compose setup provides the required runtime configuration for
local evaluation.

Configured variables in [`compose.yaml`](compose.yaml):

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/wallet
SPRING_DATASOURCE_USERNAME=wallet
SPRING_DATASOURCE_PASSWORD=wallet
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
JWT_SECRET=<local-demo-secret>
JWT_ACCESS_EXPIRY_MS=900000
JWT_REFRESH_EXPIRY_MS=604800000
APP_EVENTS_BROKER=spring
```

The secrets in `compose.yaml` are for local demo purposes only.

## Event Broker Switching

Default:

```env
APP_EVENTS_BROKER=spring
```

Kafka publisher:

```env
APP_EVENTS_BROKER=kafka
```

The Kafka publisher implementation exists in code, but Spring events remain
the active default.

## API Summary

### Auth

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`

### Wallet

- `POST /wallets`
- `GET /wallets/{walletId}`
- `GET /wallets/{walletId}/balance`

### Transactions

- `POST /wallets/{walletId}/fund`
- `POST /wallets/{walletId}/withdraw`
- `POST /transfers`
- `GET /wallets/{walletId}/transactions`
- `POST /transactions/{transactionId}/reverse`

## Testing

Run:

```bash
./mvnw.cmd test
```

Current automated coverage includes:

- wallet service unit tests
- transaction service unit tests
- idempotency aspect unit tests
- application context smoke test
