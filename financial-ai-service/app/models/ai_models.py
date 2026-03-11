from pydantic import BaseModel, Field
from typing import List, Optional, Dict

class ChatMessage(BaseModel):
    role: str = Field(pattern="^(user|assistant)$")
    content: str = Field(min_length=1, max_length=2000)

class ChatRequest(BaseModel):
    """
    AI chat request — NO PII, NO account numbers.
    Financial context is anonymized amounts + categories only.
    """
    message: str = Field(min_length=1, max_length=1000)
    session_id: str = Field(min_length=1, max_length=100)
    history: List[ChatMessage] = Field(
        default=[], max_length=20)
    financial_context: Dict[str, float] = Field(
        default={},
        description="Anonymized: category -> monthly amount")

class ChatResponse(BaseModel):
    reply: str
    session_id: str
    suggested_actions: List[str] = []

class AnomalyRequest(BaseModel):
    """Anonymized schedule data for anomaly detection."""
    schedules: List[Dict] = Field(min_length=1, max_length=500)
    historical_averages: Dict[str, float] = Field(default={})

class AnomalyResponse(BaseModel):
    anomalies: List[Dict]
    risk_level: str = Field(
        pattern="^(LOW|MEDIUM|HIGH|CRITICAL)$")

class ForecastRequest(BaseModel):
    """Anonymized spend data for forecasting."""
    monthly_data: List[Dict] = Field(
        min_length=1, max_length=24)
    forecast_months: int = Field(default=6, ge=1, le=12)

class ForecastResponse(BaseModel):
    forecasts: List[Dict]
    trend: str
    confidence: float = Field(ge=0.0, le=1.0)
    total_projected: float

class BudgetRequest(BaseModel):
    """Anonymized financial data for budget recommendations."""
    monthly_income: Optional[float] = Field(
        default=None, gt=0)
    current_obligations: Dict[str, float] = Field(default={})
    savings_goals: List[Dict] = Field(default=[])

class BudgetResponse(BaseModel):
    recommendations: List[str]
    suggested_budget: Dict[str, float]
    savings_potential: float
    debt_payoff_plan: Optional[List[Dict]] = None
