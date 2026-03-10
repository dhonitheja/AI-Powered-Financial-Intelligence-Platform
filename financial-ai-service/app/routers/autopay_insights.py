from fastapi import APIRouter, Depends
from app.middleware.auth_middleware import verify_internal_secret
from app.models.autopay_models import (
    AnalyzeRequest, AnalyzeResponse,
    CategorizeRequest, CategorizeResponse,
    BenchmarkResponse, PaymentCategory, PaymentFrequency
)
from app.services.gemini_service import (
    categorize_payment, generate_insights)

router = APIRouter(
    dependencies=[Depends(verify_internal_secret)])

FREQUENCY_TO_MONTHLY = {
    "DAILY":     30.44,
    "WEEKLY":    4.33,
    "BIWEEKLY":  2.17,
    "MONTHLY":   1.0,
    "QUARTERLY": 1/3,
    "ANNUALLY":  1/12
}

# Static benchmarks (replace with DB-backed data in V13+)
BENCHMARKS = {
    "HOME_LOAN":        {"avg": 1850, "p25": 1200, "p75": 2400,
        "tip": "Consider refinancing if rate > 7%"},
    "AUTO_LOAN":        {"avg": 520,  "p25": 350,  "p75": 750,
        "tip": "Extra payments reduce interest significantly"},
    "HEALTH_INSURANCE": {"avg": 450,  "p25": 280,  "p75": 650,
        "tip": "Compare marketplace plans annually"},
    "AUTO_INSURANCE":   {"avg": 165,  "p25": 110,  "p75": 220,
        "tip": "Bundle with home insurance for savings"},
    "HOME_INSURANCE":   {"avg": 140,  "p25": 95,   "p75": 190,
        "tip": "Bundle with auto insurance for discounts"},
    "SUBSCRIPTION":     {"avg": 85,   "p25": 45,   "p75": 130,
        "tip": "Audit subscriptions quarterly for unused ones"},
    "UTILITY":          {"avg": 210,  "p25": 140,  "p75": 290,
        "tip": "Energy audit can reduce bills 15-25%"},
    "RENT":             {"avg": 1650, "p25": 1100, "p75": 2200,
        "tip": "Negotiate lease renewal 60 days early"},
}

@router.post("/analyze", response_model=AnalyzeResponse)
async def analyze_schedules(request: AnalyzeRequest):
    """
    Analyze anonymized autopay schedules.
    Input: amounts + categories ONLY. Zero PII.
    """
    active = [s for s in request.schedules if s.is_active]

    # Calculate burn rates
    monthly_burn = sum(
        s.amount * FREQUENCY_TO_MONTHLY[s.frequency.value]
        for s in active
    )
    annual_projection = monthly_burn * 12

    # Category breakdown (monthly equivalent per category)
    category_breakdown: dict = {}
    for s in active:
        monthly = s.amount * FREQUENCY_TO_MONTHLY[s.frequency.value]
        cat = s.category.value
        category_breakdown[cat] = (
            category_breakdown.get(cat, 0) + monthly)

    # Payment health score (0–100)
    health_score = calculate_health_score(active, monthly_burn)

    # Anonymized summary for Gemini — no PII
    summary = [
        {"category": s.category.value,
         "monthly_amount": round(
             s.amount * FREQUENCY_TO_MONTHLY[s.frequency.value], 2)}
        for s in active
    ]

    # Gemini-powered insights
    ai_insights = await generate_insights(str(summary))

    return AnalyzeResponse(
        monthly_burn_rate=round(monthly_burn, 2),
        annual_projection=round(annual_projection, 2),
        category_breakdown={
            k: round(v, 2) for k, v in category_breakdown.items()},
        optimization_tips=ai_insights.get("optimization_tips", []),
        payment_health_score=health_score,
        risk_flags=ai_insights.get("risk_flags", [])
            + detect_risk_flags(active),
        savings_opportunities=ai_insights.get(
            "savings_opportunities", [])
    )

@router.post("/categorize", response_model=CategorizeResponse)
async def categorize(request: CategorizeRequest):
    """
    Categorize a payment by description.
    Input: description string ONLY. No account numbers.
    """
    result = await categorize_payment(request.description)
    return CategorizeResponse(
        category=result.get("category", "CUSTOM"),
        confidence=float(result.get("confidence", 0.5))
    )

@router.get("/benchmarks/{category}",
            response_model=BenchmarkResponse)
async def get_benchmark(category: str):
    """
    Return market benchmarks for a payment category.
    Used by Wealthix UI to show "above/below average" context.
    """
    data = BENCHMARKS.get(category.upper())
    if not data:
        return BenchmarkResponse(
            category=category,
            average_amount=0,
            percentile_25=0,
            percentile_75=0,
            tip="No benchmark data available for this category"
        )
    return BenchmarkResponse(
        category=category,
        average_amount=data["avg"],
        percentile_25=data["p25"],
        percentile_75=data["p75"],
        tip=data["tip"]
    )

def calculate_health_score(schedules, monthly_burn: float) -> int:
    if not schedules:
        return 0
    score = 100
    # Deduct for high concentration in single category
    if schedules:
        max_cat = max(
            set(s.category.value for s in schedules),
            key=lambda c: sum(
                s.amount for s in schedules
                if s.category.value == c)
        )
        max_amount = sum(
            s.amount for s in schedules
            if s.category.value == max_cat)
        if monthly_burn > 0 and (max_amount / monthly_burn) > 0.6:
            score -= 20
    # Deduct for too many payments due in same week
    # (simplified — full implementation checks due_day_of_month)
    if len(schedules) > 10:
        score -= 10
    return max(0, min(100, score))

def detect_risk_flags(schedules) -> list:
    flags = []
    if len(schedules) > 15:
        flags.append(
            "High number of recurring obligations detected")
    loan_count = sum(
        1 for s in schedules
        if s.category.value in [
            "HOME_LOAN","AUTO_LOAN",
            "PERSONAL_LOAN","EDUCATION_LOAN"])
    if loan_count >= 3:
        flags.append(
            f"{loan_count} active loan payments — "
            f"review debt consolidation options")
    return flags
