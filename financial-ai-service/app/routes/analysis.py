from fastapi import APIRouter
from app.models.transaction import TransactionInput
from app.services.ai_service import ai_service
from pydantic import BaseModel

router = APIRouter(tags=["AI Analysis"])

class GeminiAnalysisResponse(BaseModel):
    category: str
    fraudRiskScore: float
    explanation: str

@router.post("/analyze", response_model=GeminiAnalysisResponse)
async def analyze_transaction(transaction: TransactionInput):
    """
    Categorize and detect fraud risk using Google Gemini AI (1.5 Flash).
    """
    result = await ai_service.analyze_transaction(transaction)
    return result
