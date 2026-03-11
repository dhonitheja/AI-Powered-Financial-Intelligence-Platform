from fastapi import APIRouter, Depends
from app.middleware.auth_middleware import verify_internal_secret
from app.models.ai_models import BudgetRequest, BudgetResponse

router = APIRouter(
    dependencies=[Depends(verify_internal_secret)])

@router.post("/budget", response_model=BudgetResponse)
async def get_budget_recommendations(request: BudgetRequest):
    """
    Generate budget recommendations.
    Input: anonymized income + category totals.
    Uses 50/30/20 rule as baseline.
    Zero PII.
    """
    total_obligations = sum(request.current_obligations.values())

    recommendations = []
    suggested_budget = {}
    savings_potential = 0.0

    if request.monthly_income:
        income = request.monthly_income
        # 50/30/20 rule
        needs_budget = income * 0.50
        wants_budget = income * 0.30
        savings_budget = income * 0.20

        suggested_budget = {
            "needs": needs_budget,
            "wants": wants_budget,
            "savings": savings_budget
        }

        if total_obligations > needs_budget:
            recommendations.append(
                "Your fixed obligations exceed 50% of "
                "income — consider reviewing subscriptions"
            )
        else:
            savings_potential = (income - total_obligations) * 0.5
            recommendations.append(
                f"You have room to save an additional "
                f"${savings_potential:.0f}/month"
            )

    # Category-specific recommendations
    for category, amount in request.current_obligations.items():
        if category == "SUBSCRIPTION" and amount > 100:
            recommendations.append(
                "Review subscriptions — you may have unused services"
            )
        if category == "CREDIT_CARD":
            recommendations.append(
                "Paying credit card in full each month avoids interest charges"
            )

    # Debt payoff plan (snowball method)
    debt_categories = [
        {"category": cat, "amount": amt}
        for cat, amt in request.current_obligations.items()
        if cat in [
            "HOME_LOAN", "AUTO_LOAN",
            "PERSONAL_LOAN", "EDUCATION_LOAN",
            "CREDIT_CARD"]
    ]
    debt_payoff_plan = None
    if debt_categories:
        # Sort by amount ascending (snowball)
        sorted_debts = sorted(
            debt_categories,
            key=lambda x: x["amount"])
        debt_payoff_plan = [
            {
                "priority": i + 1,
                "category": d["category"],
                "strategy": (
                    "Pay minimums on all others, extra payments here first"
                    if i == 0
                    else "Pay minimum"
                )
            }
            for i, d in enumerate(sorted_debts)
        ]

    return BudgetResponse(
        recommendations=recommendations,
        suggested_budget=suggested_budget,
        savings_potential=savings_potential,
        debt_payoff_plan=debt_payoff_plan
    )
