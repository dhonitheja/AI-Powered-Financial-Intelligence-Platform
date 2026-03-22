# Wealthix: Quantum-Shift Financial Intelligence
A Hybrid AI-Banking Platform built with Java Spring Boot, Python FastAPI, and Vertex AI.

Wealthix is a high-performance financial auditing platform that bridges the gap between raw banking data and executive-level financial strategy. By utilizing a Hybrid AI Router, the system intelligently shifts workloads between Gemini 1.5 Flash (for high-speed auditing) and Claude 3.5 Sonnet (for complex tax and investment reasoning).

## 🏗️ System Architecture
The platform operates as a distributed microservices ecosystem:

*   **wealthix-api (Java/Spring Boot):** The "Nervous System." Manages Plaid OAuth, 90-day transaction syncing, and HMAC Signature Verification for bank-grade webhook security.
*   **financial-ai-service (Python/FastAPI):** The "Brain." A Hybrid Router that performs a Triage Pass on financial data to determine complexity and escalate to the appropriate LLM.
*   **wealthix-ui (React/Tailwind):** The "Interface." An adaptive dashboard featuring real-time "Spending Velocity" charts and "Expert Insight" cards.

## 🧠 The "Quantum Shift" Logic
Wealthix solves the "Cost vs. Reasoning" trade-off in AI engineering by implementing a dynamic routing layer:

*   **Standard Audit (Gemini 1.5 Flash):** Triggered for routine 90-day scans. Identifies "Ghost Subscriptions" using fuzzy-matching and calculates daily burn rates with sub-second latency.
*   **Expert Analysis (Claude 3.5 Sonnet):** Automatically escalated when the Confidence Router detects a Complexity Score > 7/10 (e.g., Tax Strategy, HSA optimization, or Investment planning).

## 🛠️ Tech Stack
*   **Backend:** Java 17, Spring Boot 3, Spring Security (HMAC/JWT), Hibernate/PostgreSQL.
*   **AI/ML:** Python 3.11, FastAPI, Vertex AI SDK, Gemini 1.5 Flash, Claude 3.5 Sonnet.
*   **Banking:** Plaid API (Link, Transactions, Webhooks).
*   **DevOps:** Docker, Docker Compose, Localtunnel (for Sandbox Webhooks).
*   **Frontend:** React, TypeScript, Tailwind CSS, Lucide Icons.

## 🚀 Getting Started
### 1. Prerequisites
*   Google Cloud Project with Vertex AI API enabled.
*   Plaid Developer Account (Sandbox keys).
*   Docker & Docker Compose.

### 2. Security
*   **Webhook Integrity:** All incoming Plaid notifications are validated against an HMAC SHA-256 signature.
*   **Data Masking:** Sensitive PII is masked before being transmitted to the AI routing layer.
*   **Credential Management:** Uses Application Default Credentials (ADC) for secure Vertex AI access without hardcoded keys.
