from pydantic import BaseModel, Field, field_validator
from typing import List, Optional
from enum import Enum


class PaymentCategory(str, Enum):
    HOME_LOAN = "HOME_LOAN"
    AUTO_LOAN = "AUTO_LOAN"
    PERSONAL_LOAN = "PERSONAL_LOAN"
    EDUCATION_LOAN = "EDUCATION_LOAN"
    CREDIT_CARD = "CREDIT_CARD"
    HEALTH_INSURANCE = "HEALTH_INSURANCE"
    HOME_INSURANCE = "HOME_INSURANCE"
    AUTO_INSURANCE = "AUTO_INSURANCE"
    LIFE_INSURANCE = "LIFE_INSURANCE"
    TERM_INSURANCE = "TERM_INSURANCE"
    UTILITY = "UTILITY"
    SUBSCRIPTION = "SUBSCRIPTION"
    SIP = "SIP"
    RENT = "RENT"
    CUSTOM = "CUSTOM"


class PaymentFrequency(str, Enum):
    DAILY = "DAILY"
    WEEKLY = "WEEKLY"
    BIWEEKLY = "BIWEEKLY"
    MONTHLY = "MONTHLY"
    QUARTERLY = "QUARTERLY"
    ANNUALLY = "ANNUALLY"


class AutoPayScheduleInput(BaseModel):
    """
    Anonymised schedule data sent to the AI service.
    Security: NO account numbers, NO routing numbers, NO user PII.
    Only amounts, categories, and frequencies are included.
    """
    payment_name: str = Field(..., max_length=255)
    payment_category: PaymentCategory
    frequency: PaymentFrequency
    amount: float = Field(..., gt=0, le=1_000_000)
    currency: str = Field(default="USD", max_length=3)
    monthly_equivalent: float = Field(..., gt=0)

    @field_validator("currency")
    @classmethod
    def validate_currency(cls, v: str) -> str:
        return v.upper()[:3]


class AutoPayAnalysisRequest(BaseModel):
    """
    Request to analyse a user's full set of autopay schedules.
    Only anonymised financial data — zero PII.
    """
    schedules: List[AutoPayScheduleInput] = Field(..., min_length=1, max_length=100)


class CategoryBreakdown(BaseModel):
    category: str
    total_monthly: float
    count: int
    percentage_of_total: float


class AutoPayAnalysisResponse(BaseModel):
    monthly_burn_rate: float
    annual_projection: float
    category_breakdown: List[CategoryBreakdown]
    optimization_tips: List[str]
    payment_health_score: int = Field(..., ge=0, le=100)
    risk_flags: List[str]
    savings_opportunities: List[str]


class AutoPayCategorizationRequest(BaseModel):
    """
    Raw payment description for AI-powered category prediction.
    No account numbers or sensitive data should be in the description.
    """
    description: str = Field(..., min_length=1, max_length=500)

    @field_validator("description")
    @classmethod
    def sanitize_description(cls, v: str) -> str:
        # Strip any digit sequences longer than 4 chars (possible account number leak prevention)
        import re
        return re.sub(r'\b\d{5,}\b', '[REDACTED]', v.strip())


class AutoPayCategorizationResponse(BaseModel):
    category: PaymentCategory
    confidence: float = Field(..., ge=0.0, le=1.0)
    reasoning: str


class BenchmarkData(BaseModel):
    category: str
    average_amount: float
    median_amount: float
    currency: str
    note: str

class TransactionInput(BaseModel):
    description: str
    amount: float
    date: str  # YYYY-MM-DD
    currency: str = "USD"

class AutoPayDetectRequest(BaseModel):
    """
    Request to analyse recent transactions and detect recurring patterns.
    """
    transactions: List[TransactionInput] = Field(..., max_length=1000)

class DetectedRecurringPayment(BaseModel):
    """
    An AI-detected recurring payment suggestion.
    """
    paymentName: str
    merchantDescription: str
    averageAmount: float
    minAmount: float
    maxAmount: float
    suggestedCategory: PaymentCategory
    suggestedFrequency: PaymentFrequency
    suggestedDayOfMonth: int
    occurrenceCount: int
    confidenceScore: int = Field(..., ge=0, le=100)
    currency: str = "USD"
