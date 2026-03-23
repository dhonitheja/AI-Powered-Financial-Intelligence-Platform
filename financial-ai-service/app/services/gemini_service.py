from typing import Optional
from google import genai
from google.genai import types
from app.config.settings import settings
import json
import logging

logger = logging.getLogger(__name__)

_client: Optional[genai.Client] = None

def _get_client() -> genai.Client:
    global _client
    if _client is None:
        if not settings.gemini_api_key:
            raise RuntimeError("GEMINI_API_KEY is not set")
        _client = genai.Client(api_key=settings.gemini_api_key)
    return _client

def _model_name() -> str:
    return settings.model_name if settings.model_name else "gemini-2.0-flash"

async def call_gemini(prompt: str) -> Optional[str]:
    try:
        client = _get_client()
        response = client.models.generate_content(
            model=_model_name(),
            contents=prompt
        )
        return response.text
    except Exception as e:
        logger.error(f"[wealthix-ai] Gemini API call failed: {str(e)}")
        return None

async def categorize_payment(description: str) -> dict:
    prompt = f"""
    You are a financial categorization engine for Wealthix.
    Categorize the following payment description into exactly
    one of these categories:
    HOME_LOAN, AUTO_LOAN, PERSONAL_LOAN, EDUCATION_LOAN,
    CREDIT_CARD, HEALTH_INSURANCE, HOME_INSURANCE,
    AUTO_INSURANCE, LIFE_INSURANCE, TERM_INSURANCE,
    UTILITY, SUBSCRIPTION, SIP, RENT, CUSTOM

    Payment description: "{description}"

    Respond with ONLY a JSON object:
    {{"category": "CATEGORY_NAME", "confidence": 0.95}}
    No explanation. No markdown. JSON only.
    """
    try:
        client = _get_client()
        response = client.models.generate_content(
            model=_model_name(),
            contents=prompt,
            config=types.GenerateContentConfig(
                response_mime_type="application/json"
            )
        )
        return json.loads(response.text)
    except Exception as e:
        logger.warning(f"Categorization failed: {e}")
        return {"category": "CUSTOM", "confidence": 0.5}

async def generate_insights(schedules_summary: str) -> dict:
    prompt = f"""
    You are Wealthix AI, a financial intelligence assistant.
    Analyze these recurring payment patterns and provide insights.

    Payment data: {schedules_summary}

    Respond with ONLY a JSON object with these exact keys:
    {{
      "optimization_tips": ["tip1", "tip2", "tip3"],
      "risk_flags": ["flag1", "flag2"],
      "savings_opportunities": ["opp1", "opp2"]
    }}
    Maximum 5 items per array. Be specific and actionable.
    No markdown. JSON only.
    """
    try:
        client = _get_client()
        response = client.models.generate_content(
            model=_model_name(),
            contents=prompt,
            config=types.GenerateContentConfig(
                response_mime_type="application/json"
            )
        )
        return json.loads(response.text)
    except Exception as e:
        logger.warning(f"Insights failed: {e}")
        return {
            "optimization_tips": [],
            "risk_flags": [],
            "savings_opportunities": []
        }
