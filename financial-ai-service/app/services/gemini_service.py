from typing import Optional
import vertexai
from vertexai.generative_models import GenerativeModel, GenerationConfig
import json
import logging

logger = logging.getLogger(__name__)

# Vertex AI is initialized in main.py lifespan
# We use a lazy model accessor or just initialize here if needed, 
# but for service consistency, we assume vertexai.init() is called.

def get_model():
    return GenerativeModel("gemini-1.5-flash")

async def call_gemini(prompt: str) -> Optional[str]:
    """
    Wrapper around Vertex AI Gemini 1.5 Flash.
    """
    try:
        model = get_model()
        response = model.generate_content(prompt)
        return response.text
    except Exception as e:
        logger.error(f"[wealthix-ai] Vertex Gemini call failed: {str(e)}")
        return None

async def categorize_payment(description: str) -> dict:
    """
    Categorize a payment description using Vertex Gemini.
    """
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
    # Use GenerationConfig for strict JSON
    try:
        model = get_model()
        response = model.generate_content(
            prompt, 
            generation_config=GenerationConfig(response_mime_type="application/json")
        )
        return json.loads(response.text)
    except Exception as e:
        logger.warning(f"Categorization failed: {e}")
        return {"category": "CUSTOM", "confidence": 0.5}

async def generate_insights(schedules_summary: str) -> dict:
    """
    Generate financial insights from anonymized schedule data.
    """
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
        model = get_model()
        response = model.generate_content(
            prompt,
            generation_config=GenerationConfig(response_mime_type="application/json")
        )
        return json.loads(response.text)
    except Exception as e:
        logger.warning(f"Insights failed: {e}")
        return {
            "optimization_tips": [],
            "risk_flags": [],
            "savings_opportunities": []
        }
