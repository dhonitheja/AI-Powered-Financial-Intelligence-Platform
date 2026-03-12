# Test Results — Pre-Phase 11 Quality Sprint

**Date:** 2026-03-12
**Sprint:** Full End-to-End Testing (Task 1)

---

## Summary

| Layer | Tool | Tests Run | Passed | Failed | Status |
|-------|------|-----------|--------|--------|--------|
| Java API (`financial-intelligence-api`) | Maven / JUnit 5 + MockMvc | 45 | 45 | 0 | ✅ All green |
| Python AI Service (`financial-ai-service`) | pytest | 18 | 18 | 0 | ✅ All green |
| Frontend (`antigravity-ui`) | Jest / React Testing Library | 10 | 10 | 0 | ✅ All green |
| **Total** | | **73** | **73** | **0** | ✅ |

---

## Java API Tests (`financial-intelligence-api`)

**Run command:** `./mvnw test`
**Result:** 45/45 passing in 15.5 s

### Test Classes

| Class | Tests | Notes |
|-------|-------|-------|
| `AutoPayServiceTest` | 12 | Schedule CRUD, ownership enforcement, pagination |
| `AutoPaySchedulerTest` | 3 | Cron: skip inactive, skip non-auto-execute, process due schedules |
| `StripePaymentServiceTest` | 7 | Payment method attach/detach, execute, ownership checks |
| `StripeWebhookTest` | 8 | Webhook signature verification, event handling |
| `AiAssistantServiceTest` | 7 | Chat, savings goals ownership, anomaly/forecast/budget delegation |
| `ExportServiceTest` | 3 | CSV export, PDF generation, PII masking in reports |
| `AutoPayIdorTest` | 5 | **IDOR security**: user B cannot read/update/delete/execute/toggle user A's schedule |

### Key Bugs Fixed During Testing

1. **`GlobalExceptionHandler` swallowed `ResponseStatusException`** — catch-all `Exception.class` handler intercepted Spring's `ResponseStatusException` and converted it to HTTP 500. Added explicit `@ExceptionHandler(ResponseStatusException.class)` before the catch-all. This was a production bug affecting all error responses.

2. **Duplicate Hibernate entity mapping** — `notification.entity.Notification` and `notification.model.entity.Notification` both registered with the same name, causing `DuplicateMappingException` at context startup. Fixed by adding `@Entity(name = "NotificationRecord")` to the unused duplicate.

3. **Java 23 / Byte Buddy incompatibility** — Mockito inline mocking fails on Java 23 without experimental flag. Added `-Dnet.bytebuddy.experimental=true` to maven-surefire-plugin argLine.

---

## Python AI Service Tests (`financial-ai-service`)

**Run command:** `venv/Scripts/python -m pytest tests/ -v`
**Result:** 18/18 passing in 1.64 s

### Test Files

| File | Tests | Coverage |
|------|-------|----------|
| `test_anomaly_detection.py` | 3 | Amount spike, concentration risk, normal data baseline |
| `test_assistant.py` | 3 | Chat reply, Gemini failure fallback, no PII in prompt |
| `test_auth.py` | 3 | Missing secret → 403, wrong secret → 403, correct secret passes |
| `test_autopay_insights.py` | 6 | Analyze, empty schedules → 400, categorize, benchmarks, health score |
| `test_forecast.py` | 3 | Forecast months count, trend classification, stable trend |

### Key Fixes During Testing

- Tests were patching `app.routers.assistant.model` (didn't exist); corrected to `app.services.gemini_service.call_gemini`
- Forecast tests used wrong endpoint (`/predict` → `/forecast`), wrong field names (`monthly_totals` → `monthly_data`, `months_ahead` → `forecast_months`), and wrong response key (`forecast` → `forecasts`) and wrong case for trend values (`increasing` → `INCREASING`)
- `ChatRequest` requires `session_id` field; tests were omitting it → 422

---

## Frontend Tests (`antigravity-ui`)

**Run command:** `npm test`
**Result:** 10/10 passing in 2.2 s

### Test Files

| File | Tests | Coverage |
|------|-------|----------|
| `tests/services/autopayService.test.ts` | 4 | No localStorage storage, no sessionStorage storage, JWT header attached, throws on 401 |
| `tests/components/autopay/AutoPayCard.test.tsx` | 6 | Renders name, masked account, status badge, delete button, no full account number, toggle handler |

### Key Fixes During Testing

- `jest.config.js` had typo `setupFilesAfterFramework` → corrected to `setupFilesAfterEnv` (caused `toBeInTheDocument` to not be available)
- `mockSchedule` used `isActive` field but interface declares `active`; added missing required fields (`categoryDisplayName`, `hasRoutingNumber`, `hasNotes`, `monthlyEquivalent`, `dueDayOfMonth`)
- Service test called `autoPayService.getSchedules()` but actual export is `listSchedules()`

---

## Security Verification

### IDOR Protection (Java)
All 5 IDOR tests pass — user B cannot access user A's resources via any endpoint:
- `GET /api/v1/autopay/schedules/{id}` → 403
- `PUT /api/v1/autopay/schedules/{id}` → 403
- `DELETE /api/v1/autopay/schedules/{id}` → 403
- `POST /api/v1/autopay/schedules/{id}/execute` → 403
- `PATCH /api/v1/autopay/schedules/{id}/toggle` → 403

### No PII in AI Prompts (Python)
`test_no_pii_in_prompt_construction` confirms no email addresses or long digit sequences appear in prompts sent to Gemini.

### No Financial Data in Browser Storage (Frontend)
`autopayService.test.ts` confirms API responses are never persisted to `localStorage` or `sessionStorage`.

### Account Number Masking (Frontend)
`AutoPayCard` component only renders `****XXXX` format; full account numbers never appear in DOM.

---

## Infrastructure Changes

| File | Change |
|------|--------|
| `financial-intelligence-api/pom.xml` | Added `-Dnet.bytebuddy.experimental=true` (Java 23 Mockito fix); added H2 test scope dependency |
| `financial-intelligence-api/src/main/java/.../GlobalExceptionHandler.java` | Added `ResponseStatusException` handler (production bug fix) |
| `financial-intelligence-api/src/main/java/.../notification/model/entity/Notification.java` | Added `@Entity(name = "NotificationRecord")` to resolve duplicate mapping |
| `antigravity-ui/jest.config.js` | Fixed `setupFilesAfterFramework` → `setupFilesAfterEnv` |
