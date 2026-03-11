from fastapi import APIRouter, Depends
from app.middleware.auth_middleware import verify_internal_secret
from app.models.ai_models import ChatRequest, ChatResponse
from app.services.gemini_service import call_gemini
from typing import List
import json

router = APIRouter(
    dependencies=[Depends(verify_internal_secret)])

WEALTHIX_SYSTEM_PROMPT = """
You are Wealthix AI Assistant, an intelligent financial
advisor embedded in the Wealthix platform.

Your capabilities:
- Answer questions about recurring payments and budgets
- Explain financial concepts clearly
- Suggest ways to optimize recurring expenses
- Help users understand their payment health score
- Provide debt payoff strategies

Rules you MUST follow:
- NEVER ask for or mention account numbers, SSN,
  card numbers, or any sensitive identifiers
- Only reference anonymized financial patterns
  (amounts and categories) provided in context
- Always recommend consulting a licensed financial
  advisor for major decisions
- Keep responses concise (under 200 words)
- Be encouraging and supportive, never alarming
- If asked about specific transactions, explain you
  can only see anonymized summaries

You are NOT a licensed financial advisor. Always
include this disclaimer for specific investment advice.
"""

def generate_suggestions(message: str) -> List[str]:
    message_lower = message.lower()
    if any(w in message_lower
           for w in ["save", "saving", "savings"]):
        return [
            "View my savings goals",
            "Show spending optimization tips",
            "Calculate my monthly surplus"
        ]
    if any(w in message_lower
           for w in ["loan", "debt", "emi"]):
        return [
            "View debt payoff plan",
            "Compare loan payments",
            "Check payment health score"
        ]
    return [
        "Analyze my spending",
        "Show upcoming payments",
        "View AI insights"
    ]

@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """
    Wealthix AI chat endpoint.
    Anonymized financial context only — zero PII.
    """
    # Build conversation history
    history_text = "\n".join([
        f"{msg.role.upper()}: {msg.content}"
        for msg in request.history[-10:]  # last 10 messages
    ])

    # Anonymized financial context
    context_text = ""
    if request.financial_context:
        context_text = (
            f"\nUser's anonymized monthly obligations: "
            f"{json.dumps(request.financial_context)}"
        )

    prompt = f"""
{WEALTHIX_SYSTEM_PROMPT}
{context_text}

Conversation history:
{history_text}

USER: {request.message}

ASSISTANT:"""

    reply = await call_gemini(prompt)
    if not reply:
        reply = (
            "I'm having trouble connecting right now. "
            "Please try again in a moment."
        )

    # Generate suggested actions based on context
    suggested = generate_suggestions(request.message)

    return ChatResponse(
        reply=reply.strip(),
        session_id=request.session_id,
        suggested_actions=suggested
    )
