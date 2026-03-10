from pydantic import BaseModel, Field, field_validator
from typing import List, Dict, Optional
from enum import Enum

class PaymentFrequency(str, Enum):
    DAILY      = "DAILY"
    WEEKLY     = "WEEKLY"
    BIWEEKLY   = "BIWEEKLY"
    MONTHLY    = "MONTHLY"
    QUARTERLY  = "QUARTERLY"
    ANNUALLY   = "ANNUALLY"

class PaymentCategory(str, Enum):
    HOME_LOAN        = "HOME_LOAN"
    AUTO_LOAN        = "AUTO_LOAN"
    PERSONAL_LOAN    = "PERSONAL_LOAN"
    EDUCATION_LOAN   = "EDUCATION_LOAN"
    CREDIT_CARD      = "CREDIT_CARD"
    HEALTH_INSURANCE = "HEALTH_INSURANCE"
    HOME_INSURANCE   = "HOME_INSURANCE"
    AUTO_INSURANCE   = "AUTO_INSURANCE"
    LIFE_INSURANCE   = "LIFE_INSURANCE"
    TERM_INSURANCE   = "TERM_INSURANCE"
    UTILITY          = "UTILITY"
    SUBSCRIPTION     = "SUBSCRIPTION"
    SIP              = "SIP"
    RENT             = "RENT"
    CUSTOM           = "CUSTOM"

class ScheduleInput(BaseModel):
    """
    Anonymized schedule data only.
    NO account numbers. NO user PII. NO names.
    """
    category:   PaymentCategory
    amount:     float = Field(gt=0, le=1_000_000)
    frequency:  PaymentFrequency
    currency:   str   = Field(default="USD", max_length=3)
    is_active:  bool  = True

    @field_validator("currency")
    @classmethod
    def validate_currency(cls, v: str) -> str:
        allowed = {"USD", "EUR", "GBP", "INR", "CAD", "AUD"}
        if v.upper() not in allowed:
            raise ValueError(f"Unsupported currency: {v}")
        return v.upper()

class AnalyzeRequest(BaseModel):
    schedules: List[ScheduleInput] = Field(
        min_length=1, max_length=500)

class AnalyzeResponse(BaseModel):
    monthly_burn_rate:      float
    annual_projection:      float
    category_breakdown:     Dict[str, float]
    optimization_tips:      List[str]
    payment_health_score:   int = Field(ge=0, le=100)
    risk_flags:             List[str]
    savings_opportunities:  List[str]

class CategorizeRequest(BaseModel):
    description: str = Field(min_length=1, max_length=500)

class CategorizeResponse(BaseModel):
    category:   PaymentCategory
    confidence: float = Field(ge=0.0, le=1.0)

class BenchmarkResponse(BaseModel):
    category:       str
    average_amount: float
    percentile_25:  float
    percentile_75:  float
    user_amount:    Optional[float] = None
    tip:            str
