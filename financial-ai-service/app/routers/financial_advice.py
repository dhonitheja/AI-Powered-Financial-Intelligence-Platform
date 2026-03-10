from fastapi import APIRouter, Depends
from pydantic import BaseModel, Field
from app.middleware.auth_middleware import verify_internal_secret

router = APIRouter(
    dependencies=[Depends(verify_internal_secret)])

class AdviceRequest(BaseModel):
    query: str = Field(..., min_length=1, max_length=1000)

class AdviceResponse(BaseModel):
    advice: str

@router.post("/ask", response_model=AdviceResponse)
async def ask_advisor(request: AdviceRequest):
    """
    General financial advice endpoint.
    Placeholder until full implementation.
    """
    return AdviceResponse(advice="This is placeholder financial advice.")
