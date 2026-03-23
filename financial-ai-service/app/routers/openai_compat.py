"""
OpenAI-compatible /openai/chat/completions endpoint.
Allows the Java AIClientService (which speaks OpenAI API format) to call
our Gemini-backed Python service without any Java code changes.
"""
from fastapi import APIRouter, Request
from app.services.gemini_service import call_gemini
import logging, uuid, time

logger = logging.getLogger(__name__)
router = APIRouter()

@router.post("/chat/completions")
async def chat_completions(request: Request):
    body = await request.json()
    messages = body.get("messages", [])

    # Build a single prompt from system + user messages
    prompt_parts = []
    for m in messages:
        role = m.get("role", "user")
        content = m.get("content", "")
        if role == "system":
            prompt_parts.append(f"[SYSTEM]: {content}")
        else:
            prompt_parts.append(f"[USER]: {content}")
    prompt = "\n\n".join(prompt_parts)

    reply = await call_gemini(prompt)
    if not reply:
        reply = "I'm sorry, I couldn't process your request right now."

    # Return OpenAI-compatible response shape
    return {
        "id": f"chatcmpl-{uuid.uuid4().hex[:8]}",
        "object": "chat.completion",
        "created": int(time.time()),
        "model": "gemini-2.5-flash",
        "choices": [{
            "index": 0,
            "message": {"role": "assistant", "content": reply},
            "finish_reason": "stop"
        }],
        "usage": {"prompt_tokens": 0, "completion_tokens": 0, "total_tokens": 0}
    }
