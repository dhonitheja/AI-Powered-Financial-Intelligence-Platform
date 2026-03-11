from fastapi import APIRouter, Depends
from app.middleware.auth_middleware import verify_internal_secret
from app.models.ai_models import ForecastRequest, ForecastResponse
import statistics

router = APIRouter(
    dependencies=[Depends(verify_internal_secret)])

@router.post("/forecast", response_model=ForecastResponse)
async def forecast_spending(request: ForecastRequest):
    """
    Forecast future spending from anonymized historical data.
    Uses simple linear regression + Gemini for narrative.
    Zero PII — monthly totals only.
    """
    monthly_totals = [
        d.get("total", 0)
        for d in request.monthly_data
    ]

    if len(monthly_totals) < 2:
        avg = monthly_totals[0] if monthly_totals else 0
        forecasts = [
            {"month": i + 1, "projected_amount": avg}
            for i in range(request.forecast_months)
        ]
        return ForecastResponse(
            forecasts=forecasts,
            trend="STABLE",
            confidence=0.5,
            total_projected=avg * request.forecast_months
        )

    # Linear regression
    n = len(monthly_totals)
    x_mean = (n - 1) / 2
    y_mean = statistics.mean(monthly_totals)
    numerator = sum(
        (i - x_mean) * (monthly_totals[i] - y_mean)
        for i in range(n))
    denominator = sum(
        (i - x_mean) ** 2 for i in range(n))
    slope = numerator / denominator if denominator else 0
    intercept = y_mean - slope * x_mean

    forecasts = []
    for i in range(request.forecast_months):
        x = n + i
        projected = max(0, intercept + slope * x)
        forecasts.append({
            "month": i + 1,
            "projected_amount": round(projected, 2)
        })

    trend = (
        "INCREASING" if slope > 10
        else "DECREASING" if slope < -10
        else "STABLE"
    )
    total_projected = sum(
        f["projected_amount"] for f in forecasts)

    # Confidence based on data consistency
    std_dev = statistics.stdev(monthly_totals)
    cv = std_dev / y_mean if y_mean > 0 else 1
    confidence = max(0.3, min(0.95, 1 - cv))

    return ForecastResponse(
        forecasts=forecasts,
        trend=trend,
        confidence=round(confidence, 2),
        total_projected=round(total_projected, 2)
    )
