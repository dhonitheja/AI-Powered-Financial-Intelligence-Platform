# AI‑Powered Financial Intelligence Platform

A full‑stack, production‑ready platform that integrates with **Plaid** to ingest bank transactions, enrich them with AI‑generated insights, and expose a secure REST API for a modern React UI.

## Table of Contents
- [Features](#features)
- [Architecture Overview](#architecture-overview)
- [Security Hardening](#security-hardening)
- [Getting Started](#getting-started)
- [Running Locally](#running-locally)
- [Database Migrations](#database-migrations)
- [Testing](#testing)
- [Environment Variables](#environment-variables)
- [License](#license)

## Features
- **Plaid Integration** – Link, exchange public token, and incremental `/transactions/sync`.
- **AES‑256‑GCM Encryption** – Plaid access tokens are stored encrypted with a random IV.
- **Cursor‑based Sync** – Efficient incremental sync using `next_cursor`.
- **Idempotent Transaction Upserts** – No duplicate `plaid_transaction_id`s.
- **Soft‑Delete** – Removed Plaid transactions are marked `deleted = true`.
- **JWT Authentication** – All `/api/plaid/**` endpoints are protected.
- **Rate Limiting** – Prevents abuse of the `/plaid/sync` endpoint.
- **Comprehensive Logging** – Start/end of sync, counts of added/updated/removed rows, cursor updates, and error details (no secret leakage).
- **Integration Tests** – Full‑sync, idempotency, random‑IV validation, and failure‑rollback scenarios.

## Architecture Overview
```text
+-------------------+      +-------------------+      +-------------------+
|   Antigravity UI  | <--->|  financial‑ai‑service (FastAPI) |
+-------------------+      +-------------------+      +-------------------+
                                   |
                                   v
                         +-------------------+
                         | financial‑intelligence‑api (Spring Boot) |
                         +-------------------+
                                   |
                                   v
                         +-------------------+
                         |      PostgreSQL (Supabase)          |
                         +-------------------+
```

## Security Hardening
1. **Encrypted Access Token** – `EncryptionService` uses **AES‑256‑GCM** with a per‑record random IV. Tokens are never logged.
2. **Legacy Migration** – `V4__ReEncryptAccessTokens` migrates any existing tokens encrypted with the old static‑IV CBC scheme.
3. **Unique Transaction IDs** – DB constraint on `transactions.plaid_transaction_id`.
4. **Soft‑Delete Flag** – `transactions.deleted` column.
5. **JWT‑Only Auth** – No `userId` accepted from the client; resolved from `SecurityContext`.
6. **Rate Limiting** – Configured on `/plaid/sync`.
7. **Observability** – Structured logs for every sync step.

## Getting Started

### Prerequisites
- **Java 17+**, **Maven**, **Node.js 18+**, **Docker** (optional).
- **Plaid sandbox credentials** (client ID, secret, public key).
- **Supabase/PostgreSQL** connection string.
- **Environment file** (`.env`) – **DO NOT** commit this file! It is ignored by `.gitignore`.

### Environment Variables (`.env`)
```env
PLAID_CLIENT_ID=your_client_id
PLAID_SECRET=your_secret
PLAID_PUBLIC_KEY=your_public_key
PLAID_ENCRYPTION_KEY=your_base64_32_byte_key   # 256‑bit key for AES‑GCM
JWT_SECRET=your_jwt_secret
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>/<db>
SPRING_DATASOURCE_USERNAME=your_username
SPRING_DATASOURCE_PASSWORD=your_password
```

### Running Locally
```bash
# Backend (Spring Boot)
cd financial-intelligence-api
./mvnw spring-boot:run

# AI Service (FastAPI)
cd ../financial-ai-service
python -m venv venv
source venv/bin/activate   # Windows: venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload

# Frontend (Next.js)
cd ../antigravity-ui
npm install
npm run dev
```

## Database Migrations
We use **Flyway**. On application start it will automatically apply:
- `V2__add_encrypted_access_token_and_next_cursor.sql`
- `V3__add_soft_delete_to_transactions.sql`
- `V4__ReEncryptAccessTokens.java` (one‑time migration)

To run migrations manually:
```bash
./mvnw flyway:migrate
```

## Testing
```bash
# Run all unit & integration tests
./mvnw test
```
The `IntegrationPlaidSyncTest` covers:
- Full sync + idempotency
- Random IV property
- Failure handling with rollback

## License
MIT – feel free to fork, modify, and deploy.
