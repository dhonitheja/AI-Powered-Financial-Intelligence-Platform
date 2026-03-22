from fastapi import APIRouter, Depends
from app.middleware.auth_middleware import verify_internal_secret
from app.models.ai_models import ChatRequest, ChatResponse
from app.services.ai_service import ai_service
from typing import List, Optional, Dict, Any
import json
import logging

logger = logging.getLogger(__name__)

router = APIRouter(
    dependencies=[Depends(verify_internal_secret)])

@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """
    Upgraded Jass AI chat endpoint with RAG support.
    """
    # Combine query with history if needed, but query_assistant handles retrieval
    query = request.message
    user_id = request.session_id # Using session_id as a proxy for user_id in this context
    
    # Use the upgraded AIService with RAG
    result = await ai_service.query_assistant(query, user_id=user_id)
    
    return ChatResponse(
        reply=result["answer"],
        session_id=request.session_id,
        suggested_actions=result["suggestions"],
        metrics=result.get("analysis_metrics", {})  # Default to empty dict, never None
    )

@router.post("/ingest")
async def ingest_data(data: Dict[str, Any]):
    """
    Ingest user transaction data into RAG vector store.
    """
    user_id = data.get("user_id")
    transactions = data.get("transactions", [])
    
    if not user_id:
        return {"status": "error", "message": "user_id is required"}
        
    await ai_service.ingest_user_data(user_id, transactions)
    return {"status": "success", "message": f"Ingested {len(transactions)} transactions for RAG"}

@router.post("/analyze")
async def analyze_finances(data: Dict[str, Any]):
    """
    Internal endpoint for batch analysis.
    """
    user_id = data.get("user_id")
    transactions = data.get("transactions", [])
    user_query = data.get("user_query") # Extract user query if present
    
    if not user_id:
        return {"status": "error", "message": "user_id is required"}
    
    # analyze_finances is sync — call directly (no await)
    report = ai_service.analyze_finances(user_id, transactions, user_query)
    return report
