# Wealthix Deployment Guide

## Architecture

```
                        ┌──────────────┐
                        │   Nginx /    │
                        │  CloudFlare  │
                        └──────┬───────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
        ┌─────▼─────┐   ┌─────▼─────┐   ┌─────▼─────┐
        │ wealthix-  │   │ wealthix- │   │ wealthix- │
        │    ui      │   │    api    │   │    ai     │
        │ (Next.js)  │   │ (Spring)  │   │ (FastAPI) │
        │  :3000     │   │  :8080    │   │  :8000    │
        └────────────┘   └─────┬─────┘   └───────────┘
                               │
                        ┌──────▼───────┐
                        │  PostgreSQL  │
                        │  (Supabase)  │
                        └──────────────┘
```

## Prerequisites

- Docker & Docker Compose v2
- PostgreSQL 15+ (Supabase hosted)
- Stripe account (test or live keys)
- Plaid developer account
- Google AI API key (Gemini)

## Quick Start (Local Development)

```bash
# 1. Copy and fill environment variables
cp .env.example .env
# Edit .env with your actual keys

# 2. Build and start all services
docker compose up --build -d

# 3. Check health
curl http://localhost:8080/actuator/health
curl http://localhost:8000/health
curl http://localhost:3000/api/health
```

## Environment Variables

See `.env.example` for the full list. **NEVER commit `.env` to git.**

| Variable | Service | Description |
|---|---|---|
| `DATABASE_URL` | API | PostgreSQL connection string |
| `JWT_SECRET` | API | 64-byte base64 secret for JWT signing |
| `ENCRYPTION_KEY` | API | 32-byte AES-256-GCM key |
| `STRIPE_SECRET_KEY` | API | Stripe secret key (`sk_live_...`) |
| `STRIPE_WEBHOOK_SECRET` | API | Stripe webhook signing secret |
| `PLAID_CLIENT_ID` | API | Plaid client ID |
| `PLAID_SECRET` | API | Plaid secret key |
| `GOOGLE_API_KEY` | AI | Google Generative AI API key |
| `INTERNAL_AI_SERVICE_SECRET` | API, AI | Shared secret for service-to-service auth |
| `NEXT_PUBLIC_API_URL` | UI | Public API base URL |

## Docker Compose Services

| Service | Port | Health Check |
|---|---|---|
| `wealthix-api` | 8080 | `/actuator/health` |
| `wealthix-ai` | 8000 | `/health` |
| `wealthix-ui` | 3000 | `/api/health` |

All containers run as **non-root** users.

## Database Migrations

Migrations are in `financial-intelligence-api/src/main/resources/db/migration/`.

They run automatically on API startup via `DatabaseMigrationRunner`.

| Migration | Purpose |
|---|---|
| V10 | AutoPay Hub schema |
| V11 | Stripe fields |
| V12 | Audit logs |
| V13 | Performance indexes |
| V14 | Notifications table |
| V15 | Onboarding field |

## Production Deployment Checklist

### Pre-Deploy
- [ ] All tests pass (`mvn test`, `pytest`, `npm run build`)
- [ ] No hardcoded secrets: `grep -r "sk_live\|sk_test\|whsec_" --include="*.java" --include="*.ts" --include="*.py"`
- [ ] OWASP dependency check passes
- [ ] npm audit has no high/critical vulnerabilities
- [ ] Docker images build successfully
- [ ] Non-root users verified in all containers

### Deploy
1. Build production Docker images
2. Push to container registry
3. Run database migrations (auto on startup)
4. Deploy containers with health checks
5. Verify health endpoints return OK
6. Test Stripe webhook delivery
7. Verify Plaid sandbox → production keys

### Post-Deploy
- [ ] Monitor `/actuator/prometheus` metrics
- [ ] Check structured logs for errors
- [ ] Verify notification delivery
- [ ] Test AutoPay execution flow end-to-end

## Security Checklist

- [x] AES-256-GCM encryption on all sensitive fields
- [x] JWT authentication with HTTP-only cookies
- [x] IDOR protection on all user-scoped queries
- [x] Stripe webhook signature verification
- [x] Non-root Docker containers
- [x] CSP headers on frontend
- [x] Rate limiting on write endpoints
- [x] PII scrubbing in all logs and notifications
- [x] Service-to-service auth (`X-Wealthix-Service-Secret`)
- [x] No sensitive data in API responses (masked account numbers)

## Monitoring

### Spring Boot Actuator Endpoints
- `GET /actuator/health` — liveness + readiness + custom indicators
- `GET /actuator/prometheus` — Prometheus-compatible metrics
- `GET /actuator/info` — application metadata

### Custom Health Indicators
- **EncryptionHealthIndicator** — verifies AES-256-GCM is operational
- **AiServiceHealthIndicator** — checks AI service reachability (DEGRADED, not DOWN)

### Logging
- **Production**: JSON structured logs (Logstash encoder)
- **Development**: Console pattern logs
- All services scrub PII before logging

## Performance

- HikariCP connection pool (10 max, 5 min idle)
- JPA batch inserts/updates (batch size 20)
- Caffeine cache (dashboard data, 5m TTL)
- Database indexes on hot query paths
- Next.js standalone build with security headers
- FastAPI LRU cache on benchmark data

## CI/CD

- `.github/workflows/ci.yml` — runs on push/PR to main
- `.github/workflows/deploy.yml` — deploys on tag push

## Rollback

```bash
# Roll back to previous image
docker compose down
docker compose up -d --force-recreate
```

For database rollbacks, create a down-migration SQL and apply manually.
