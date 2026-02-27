from fastapi import APIRouter
from app.models.assistant import AssistantQuery, AssistantResponse
from app.services.ai_service import ai_service

router = APIRouter(prefix="/assistant", tags=["assistant"])

@router.post("/analyze", response_model=AssistantResponse)
async def analyze_assistant(payload: AssistantQuery):
    result = await ai_service.query_assistant(payload.query, payload.context)
    return result
