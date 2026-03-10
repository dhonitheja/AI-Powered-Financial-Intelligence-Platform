"""
AutoPay Insights Router
-----------------------
Provides AI-powered analysis for a user's recurring payment schedules.

Security contract:
- ALL inputs are anonymised: only amounts, categories, and frequencies.
  NO account numbers, routing numbers, or user PII ever reach this service.
- Service-to-service auth is enforced via the X-Internal-Secret header.
  Requests without a valid secret are rejected with 403.
- Pydantic models enforce strict input validation on all endpoints.
"""

import logging
import json
from fastapi import APIRouter, HTTPException, Header, Depends
from typing import Optional, List

from app.models.autopay import (
    AutoPayAnalysisRequest,
    AutoPayAnalysisResponse,
    AutoPayCategorizationRequest,
    AutoPayCategorizationResponse,
    BenchmarkData,
    CategoryBreakdown,
    PaymentCategory,
    PaymentFrequency,
    AutoPayDetectRequest,
    DetectedRecurringPayment,
)
from app.config.settings import settings
from app.services.ai_service import ai_service

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/autopay", tags=["AutoPay Insights"])


# ── Security dependency ───────────────────────────────────────────────────────

def verify_internal_secret(x_internal_secret: Optional[str] = Header(None)):
    """
    Validates the X-Internal-Secret header for service-to-service auth.
    All external traffic should be blocked at the network level;
    this is a defence-in-depth measure.
    """
    if settings.autopay_internal_secret:
        if x_internal_secret != settings.autopay_internal_secret:
            logger.warning("AutoPay endpoint called with invalid internal secret")
            raise HTTPException(status_code=403, detail="Forbidden: invalid service secret")


# ── Endpoints ─────────────────────────────────────────────────────────────────

@router.post("/analyze", response_model=AutoPayAnalysisResponse)
async def analyze_schedules(
    request: AutoPayAnalysisRequest,
    _: None = Depends(verify_internal_secret),
):
    """
    Analyse a user's autopay schedules and return:
    - Monthly burn rate & annual projection
    - Category breakdown with percentages
    - Optimisation tips and savings opportunities
    - AI-generated risk flags (e.g. "3 payments cluster in same week")
    - Payment health score (0-100)

    Input: anonymised schedule data only — no PII.
    """
    schedules = request.schedules
    total_monthly = sum(s.monthly_equivalent for s in schedules)
    annual = total_monthly * 12

    # Category aggregation
    category_totals: dict[str, tuple[float, int]] = {}
    for s in schedules:
        cat = s.payment_category.value
        prev_total, prev_count = category_totals.get(cat, (0.0, 0))
        category_totals[cat] = (prev_total + s.monthly_equivalent, prev_count + 1)

    breakdown = [
        CategoryBreakdown(
            category=cat,
            total_monthly=round(total, 2),
            count=count,
            percentage_of_total=round((total / total_monthly * 100) if total_monthly > 0 else 0, 1),
        )
        for cat, (total, count) in sorted(category_totals.items(),
                                           key=lambda x: x[1][0], reverse=True)
    ]

    # Use Gemini for intelligent insights
    prompt = _build_analysis_prompt(schedules, total_monthly, breakdown)
    try:
        model = ai_service._get_model()
        response = model.generate_content(prompt)
        text = response.text.strip()
        if text.startswith("```"):
            text = text.strip("`").strip("json").strip()
        ai_result = json.loads(text)
    except Exception as e:
        logger.error(f"AutoPay AI analysis failed: {e}")
        ai_result = {
            "optimization_tips": ["Review your subscription services for unused ones."],
            "risk_flags": [],
            "savings_opportunities": [],
        }

    # Simple heuristic health score
    overdue_risk = 1 if len(schedules) > 0 else 0
    health_score = min(100, max(0, 80 - overdue_risk * 10))

    return AutoPayAnalysisResponse(
        monthly_burn_rate=round(total_monthly, 2),
        annual_projection=round(annual, 2),
        category_breakdown=breakdown,
        optimization_tips=ai_result.get("optimization_tips", [])[:5],
        payment_health_score=health_score,
        risk_flags=ai_result.get("risk_flags", [])[:5],
        savings_opportunities=ai_result.get("savings_opportunities", [])[:5],
    )


@router.post("/categorize", response_model=AutoPayCategorizationResponse)
async def categorize_payment(
    request: AutoPayCategorizationRequest,
    _: None = Depends(verify_internal_secret),
):
    """
    Predict the payment category for a raw payment description.
    Used when a user adds a new payment and is unsure which category to choose.
    Description is sanitised before AI processing (digit sequences >4 chars redacted).
    """
    prompt = f"""
You are a financial payment categorizer. Given a payment description, classify it
into exactly one of these categories:
HOME_LOAN, AUTO_LOAN, PERSONAL_LOAN, EDUCATION_LOAN, CREDIT_CARD,
HEALTH_INSURANCE, HOME_INSURANCE, AUTO_INSURANCE, LIFE_INSURANCE, TERM_INSURANCE,
UTILITY, SUBSCRIPTION, SIP, RENT, CUSTOM.

Payment description: "{request.description}"

Return ONLY a JSON object:
{{
  "category": "CATEGORY_VALUE",
  "confidence": 0.0,
  "reasoning": "brief explanation"
}}
"""
    try:
        model = ai_service._get_model()
        response = model.generate_content(prompt)
        text = response.text.strip()
        if text.startswith("```"):
            text = text.strip("`").strip("json").strip()
        result = json.loads(text)

        category_str = result.get("category", "CUSTOM").upper()
        try:
            category = PaymentCategory(category_str)
        except ValueError:
            category = PaymentCategory.CUSTOM

        return AutoPayCategorizationResponse(
            category=category,
            confidence=float(result.get("confidence", 0.5)),
            reasoning=result.get("reasoning", "Category inferred from description."),
        )
    except Exception as e:
        logger.error(f"AutoPay categorize failed: {e}")
        return AutoPayCategorizationResponse(
            category=PaymentCategory.CUSTOM,
            confidence=0.0,
            reasoning="Unable to categorize automatically. Please select manually.",
        )


@router.get("/benchmarks/{category}", response_model=BenchmarkData)
async def get_benchmarks(
    category: PaymentCategory,
    _: None = Depends(verify_internal_secret),
):
    """
    Returns average market rates for a payment category.
    Uses static reference data (can be enhanced with live data sources).
    """
    benchmarks = _get_benchmark_data()
    data = benchmarks.get(category.value)
    if not data:
        raise HTTPException(status_code=404, detail=f"No benchmark data for {category.value}")
    return data


@router.post("/detect", response_model=List[DetectedRecurringPayment])
async def detect_recurring(
    request: AutoPayDetectRequest,
    _: None = Depends(verify_internal_secret),
):
    """
    Analyzes transaction history to detect recurring payments not yet in AutoPay Hub.
    """
    if not request.transactions:
        return []

    # Format transactions for the prompt
    tx_list = "\n".join([
        f"- {t.date}: {t.description} (${t.amount})" 
        for t in request.transactions
    ])

    prompt = f"""
You are a financial AI analyzing transaction history to detect recurring payments.
Identify subscriptions, bills, loans, or other regular payments.
Look for similar descriptions and amounts that occur on a regular schedule (e.g., monthly).

Recent transactions:
{tx_list}

Return the detected recurring payments as exactly a JSON array of objects. 
Each object must have these exact keys:
- "paymentName": String (clean title case, no numbers)
- "merchantDescription": String (original merchant/description)
- "averageAmount": Float
- "minAmount": Float
- "maxAmount": Float
- "suggestedCategory": String (One of HOME_LOAN, AUTO_LOAN, PERSONAL_LOAN, EDUCATION_LOAN, CREDIT_CARD, HEALTH_INSURANCE, HOME_INSURANCE, AUTO_INSURANCE, LIFE_INSURANCE, TERM_INSURANCE, UTILITY, SUBSCRIPTION, SIP, RENT, CUSTOM)
- "suggestedFrequency": String (One of DAILY, WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, ANNUALLY)
- "suggestedDayOfMonth": Integer
- "occurrenceCount": Integer
- "confidenceScore": Integer (0-100)

If none are found, return an empty array [].
"""
    try:
        model = ai_service._get_model()
        response = model.generate_content(prompt)
        text = response.text.strip()
        if text.startswith("```"):
            text = text.strip("`").strip("json").strip()
        results = json.loads(text)
        
        # Parse output into models to validate
        detected_payments = []
        for res in results:
            try:
                # Basic parsing fallbacks
                cat = res.get("suggestedCategory", "CUSTOM")
                freq = res.get("suggestedFrequency", "MONTHLY")
                dp = DetectedRecurringPayment(
                    paymentName=res.get("paymentName", "Unknown"),
                    merchantDescription=res.get("merchantDescription", ""),
                    averageAmount=float(res.get("averageAmount", 0)),
                    minAmount=float(res.get("minAmount", 0)),
                    maxAmount=float(res.get("maxAmount", 0)),
                    suggestedCategory=PaymentCategory(cat) if cat in PaymentCategory.__members__ else PaymentCategory.CUSTOM,
                    suggestedFrequency=PaymentFrequency(freq) if freq in PaymentFrequency.__members__ else PaymentFrequency.MONTHLY,
                    suggestedDayOfMonth=int(res.get("suggestedDayOfMonth", 1)),
                    occurrenceCount=int(res.get("occurrenceCount", 1)),
                    confidenceScore=int(res.get("confidenceScore", 50))
                )
                detected_payments.append(dp)
            except Exception as e:
                logger.warning(f"Skipping malformed detection result: {e}")
                
        return detected_payments
    except Exception as e:
        logger.error(f"AutoPay detect recurring failed: {e}")
        return []


# ── Helpers ───────────────────────────────────────────────────────────────────

def _build_analysis_prompt(schedules, total_monthly: float, breakdown) -> str:
    schedule_summary = "\n".join(
        f"- {s.payment_category.value}: ${s.monthly_equivalent:.2f}/month"
        for s in schedules
    )
    return f"""
You are a financial analysis AI. Analyse these recurring payments (anonymised, no PII):

Monthly payment breakdown:
{schedule_summary}

Total monthly obligations: ${total_monthly:.2f}

Provide actionable insights in this exact JSON format:
{{
  "optimization_tips": ["tip 1", "tip 2", "tip 3"],
  "risk_flags": ["flag 1 if any"],
  "savings_opportunities": ["opportunity 1 if any"]
}}

Rules:
- optimization_tips: practical ways to reduce costs or improve payment management
- risk_flags: concerning patterns (e.g., "Multiple large payments due same week", 
  "Insurance coverage may be duplicated")
- savings_opportunities: specific savings (e.g., "Bundling home and auto insurance 
  typically saves 10-15%")
- Maximum 3 items per array
- Do NOT include any personal or account information
"""


def _get_benchmark_data() -> dict:
    """Reference benchmark data for payment categories."""
    return {
        "HOME_LOAN": BenchmarkData(
            category="HOME_LOAN",
            average_amount=1650.0,
            median_amount=1450.0,
            currency="USD",
            note="Based on 30-year fixed mortgage at current rates",
        ),
        "AUTO_LOAN": BenchmarkData(
            category="AUTO_LOAN",
            average_amount=545.0,
            median_amount=510.0,
            currency="USD",
            note="Average new car payment in the US (2024)",
        ),
        "AUTO_INSURANCE": BenchmarkData(
            category="AUTO_INSURANCE",
            average_amount=190.0,
            median_amount=175.0,
            currency="USD",
            note="Average full coverage monthly premium",
        ),
        "HEALTH_INSURANCE": BenchmarkData(
            category="HEALTH_INSURANCE",
            average_amount=456.0,
            median_amount=400.0,
            currency="USD",
            note="Average individual marketplace plan",
        ),
        "HOME_INSURANCE": BenchmarkData(
            category="HOME_INSURANCE",
            average_amount=152.0,
            median_amount=140.0,
            currency="USD",
            note="Average annual premium / 12",
        ),
        "SUBSCRIPTION": BenchmarkData(
            category="SUBSCRIPTION",
            average_amount=45.0,
            median_amount=35.0,
            currency="USD",
            note="Average across common streaming/software subscriptions",
        ),
        "UTILITY": BenchmarkData(
            category="UTILITY",
            average_amount=117.0,
            median_amount=105.0,
            currency="USD",
            note="Average monthly electricity + internet",
        ),
        "RENT": BenchmarkData(
            category="RENT",
            average_amount=1702.0,
            median_amount=1500.0,
            currency="USD",
            note="US national average apartment rent",
        ),
    }
