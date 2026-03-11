from fastapi import APIRouter, Depends
from app.middleware.auth_middleware import verify_internal_secret
from app.models.ai_models import AnomalyRequest, AnomalyResponse

router = APIRouter(
    dependencies=[Depends(verify_internal_secret)])

@router.post("/detect", response_model=AnomalyResponse)
async def detect_anomalies(request: AnomalyRequest):
    """
    Detect unusual patterns in anonymized payment data.
    Compares current amounts vs historical averages.
    Zero PII — amounts and categories only.
    """
    anomalies = []

    for schedule in request.schedules:
        category = schedule.get("category", "UNKNOWN")
        amount = schedule.get("amount", 0)
        historical_avg = request.historical_averages.get(
            category, 0)

        # Spike detection: > 50% above historical average
        if historical_avg > 0:
            pct_change = ((amount - historical_avg)
                         / historical_avg * 100)
            if pct_change > 50:
                anomalies.append({
                    "category": category,
                    "type": "AMOUNT_SPIKE",
                    "description": (
                        f"{category} payment is "
                        f"{pct_change:.0f}% above "
                        f"your usual amount"
                    ),
                    "severity": "HIGH"
                        if pct_change > 100 else "MEDIUM"
                })

    # Concentration risk: one category > 60% of total
    total = sum(s.get("amount", 0)
                for s in request.schedules)
    if total > 0:
        by_category = {}
        for s in request.schedules:
            cat = s.get("category", "UNKNOWN")
            by_category[cat] = (
                by_category.get(cat, 0)
                + s.get("amount", 0))
        for cat, amt in by_category.items():
            if (amt / total) > 0.6:
                anomalies.append({
                    "category": cat,
                    "type": "CONCENTRATION_RISK",
                    "description": (
                        f"{cat} represents over 60% "
                        f"of your monthly obligations"
                    ),
                    "severity": "MEDIUM"
                })

    risk_level = "LOW"
    if any(a["severity"] == "HIGH" for a in anomalies):
        risk_level = "HIGH"
    elif any(a["severity"] == "MEDIUM" for a in anomalies):
        risk_level = "MEDIUM"

    return AnomalyResponse(
        anomalies=anomalies,
        risk_level=risk_level
    )
