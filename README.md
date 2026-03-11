# Wealthix — AI-Powered Financial Intelligence Platform

Wealthix is a comprehensive, next-generation financial intelligence platform that empowers users to seamlessly synthesize their financial footprint, interact with advanced AI-driven generative financial analytics, track spending securely, and intuitively uncover wealth-building insights.

This repository encapsulates the full-stack system, leveraging a robust Spring Boot backend architecture alongside a modern, dynamic Next.js frontend GUI and FastAPI-based microservices for asynchronous generative artificial intelligence querying.

## System Architecture Overview

The system abstracts into three decoupled core components:
1. **wealthix-api** (Java 17, Spring Boot 3.2.3): The monolithic core REST API. Orchestrates user authentication, CRUD operations over secure financial transaction data, Plaid integration logic, cron-scheduled AutoPay transactions, WebSocket STOMP messaging for gamification events, and exports.
2. **wealthix-ui** (Next.js 14, React, TypeScript): The frontend user-facing dashboard. Highly responsive visually, meticulously secured via CSRF/JWT interceptors, implements real-time WebSockets to toast achievements dynamically, natively integrates visual AI graphs, and features high fidelity layout/styling leveraging Tailwind CSS natively alongside Lucide React visual artifacts.
3. **wealthix-ai** (Python 3.11, FastAPI): A high-performance Python microservice optimized to perform isolated interactions with Google's Gemini language model architectures. Computes and streams advanced ML-grade risk scorings over raw transaction meta-text, detects spending anomalies mathematically, and powers the Generative Assistant interface natively inside a strictly isolated runtime sandbox.

## Security Baseline Principles
- Backend leverages **AES-256-GCM** encryption symmetrically storing sensitive credentials gracefully within the persistence database natively.
- No PII is emitted to external providers explicitly. Transaction data sent to AI contexts only ever includes randomized strings and generalized numbers—Account ID linking and specific names are masked securely.
- Cross-Site Request Forgery (CSRF) protection is mapped to HTTP-only explicit stateless session configurations utilizing encrypted JWT tokens (HS512 signing blocks).

## Development Setup

The standard environment encapsulates modern dependency suites automatically injected via Maven & NPM scripts natively. Ensure you have installed standard tools properly on your system natively including node, npm, python (pip), and java.

### 1. Database (PostgreSQL / Flyway)
Data models map onto a PostgreSQL dialect naturally inside `application-dev.yml`. We automatically provision a containerized local Postgres deployment using Flyway script iterations. Database versioning dynamically controls table architecture and constraint bounds.

### 2. Run API Backend (`wealthix-api`)
```
cd financial-intelligence-api
./mvnw clean install
./mvnw spring-boot:run
```
*(Runs on `http://localhost:8080`)*

### 3. Run AI Service (`wealthix-ai`)
```
cd financial-intelligence-ai
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```
*(Runs on `http://localhost:8000`)*

### 4. Run Frontend (`wealthix-ui`)
```
cd antigravity-ui
npm install
npm run dev
```
*(Runs on `http://localhost:3000`)*

## Features

- **Plaid Sync Engine**: Links your bank cleanly fetching transactions reliably parsing Webhook payload events properly natively.
- **AI Agent (Gemini)**: Detects hidden anomalies mathematically. Scores fraud on normalized heuristics. Forecasts dynamic month-over-month variances automatically.
- **Gamification Mechanics**: XP bounds mapping linearly awarding dynamic prestige Tiers (`SILVER`, `GOLD`... `DIAMOND`) utilizing realtime Websockets (`SockJS`).
- **Comprehensive PDF / CSV Exports**: Render beautifully organized structured transactional reports mapping dynamically generated Apache Commons data configurations intuitively out to end users securely.

## Test Harness

Run backend JUnit isolation suites automatically via Maven seamlessly ensuring core runtime logic holds true:
```
./mvnw test
```

## IDE Tooling Used

Developed seamlessly by the Antigravity Agent utilizing modern continuous automated generation iterations. All environments dynamically verified end to end seamlessly utilizing live container bindings properly natively.
