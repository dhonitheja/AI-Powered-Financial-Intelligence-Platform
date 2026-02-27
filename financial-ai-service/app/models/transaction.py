from pydantic import BaseModel, Field
from datetime import datetime
from typing import Optional

class TransactionInput(BaseModel):
    description: str
    amount: float
    transactionDate: datetime

class CategorizationResponse(BaseModel):
    category: str
    confidence: float
    suggestion: Optional[str] = None

class FraudDetectionResponse(BaseModel):
    fraud_risk_score: float = Field(..., alias="fraudRiskScore")
    risk_level: str
    flags: list[str]

    class Config:
        populate_by_name = True
