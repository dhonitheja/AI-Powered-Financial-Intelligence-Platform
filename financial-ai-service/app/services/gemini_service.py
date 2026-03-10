import os
import google.generativeai as genai
from typing import Optional

genai.configure(api_key=os.getenv("GOOGLE_API_KEY"))
model = genai.GenerativeModel("gemini-2.5-flash")

async def call_gemini(prompt: str) -> Optional[str]:
    """
    Wrapper around Gemini API.
    Always wrap in try/except.
    AI failure must NEVER break business logic.
    NEVER include PII or account numbers in prompts.
    """
    try:
        response = model.generate_content(prompt)
        return response.text
    except Exception as e:
        # Log generic failure — no sensitive data
        print(f"[wealthix-ai] Gemini call failed: {type(e).__name__}")
        return None

async def categorize_payment(description: str) -> dict:
    """
    Categorize a payment description using Gemini.
    Input: anonymized description string only.
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
    result = await call_gemini(prompt)
    if not result:
        return {"category": "CUSTOM", "confidence": 0.5}
    try:
        import json
        cleaned = result.strip().replace("```json", "").replace("```", "")
        return json.loads(cleaned)
    except Exception:
        return {"category": "CUSTOM", "confidence": 0.5}

async def generate_insights(schedules_summary: str) -> dict:
    """
    Generate financial insights from anonymized schedule data.
    Input: summary string with amounts + categories ONLY.
    """
    prompt = f"""
    You are Wealthix AI, a financial intelligence assistant.
    Analyze these recurring payment patterns and provide insights.
    Data contains amounts and categories only — no personal info.

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
    result = await call_gemini(prompt)
    if not result:
        return {
            "optimization_tips": [],
            "risk_flags": [],
            "savings_opportunities": []
        }
    try:
        import json
        cleaned = result.strip().replace("```json","").replace("```","")
        return json.loads(cleaned)
    except Exception:
        return {
            "optimization_tips": [],
            "risk_flags": [],
            "savings_opportunities": []
        }
