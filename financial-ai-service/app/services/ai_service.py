from google import genai
from google.genai import types
from anthropic import AnthropicVertex
from app.config.settings import settings
import json
import logging
from typing import List, Dict, Any, Optional
import uuid
from datetime import datetime

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("wealthix-ai")

JASS_SYSTEM_INSTRUCTION = """
You are Jass 2.0, a Quantum-Level Financial Intelligence Engine.
Your analysis architecture combines Big Four auditing precision with VC strategic foresight.
Focus on: Ghost Subscription detection, Spending Velocity, and Tax Strategy.
"""

class AIService:
    def __init__(self):
        self.project_id = "wealthix-pro"
        self.location = "us-central1"
        self._client: Optional[genai.Client] = None

    def _get_client(self) -> genai.Client:
        if self._client is None:
            if not settings.gemini_api_key:
                raise RuntimeError("GEMINI_API_KEY is not set")
            self._client = genai.Client(api_key=settings.gemini_api_key)
        return self._client

    def _model_name(self) -> str:
        return settings.model_name if settings.model_name else "gemini-2.0-flash"

    def _extract_json(self, response_text: str) -> dict:
        try:
            clean_text = response_text.replace("```json", "").replace("```", "").strip()
            return json.loads(clean_text)
        except Exception as e:
            logger.error(f"Failed to parse AI JSON: {e}")
            return {"error": "Parsing error"}

    def analyze_finances(self, user_id: str, transactions: List[Dict[str, Any]], user_query: Optional[str] = None):
        client = self._get_client()
        model = self._model_name()

        user_prompt = f"""
        USER_ID: {user_id}
        USER_QUERY: "{user_query if user_query else 'Monitor my financial health'}"
        TRANS_DATA: {json.dumps(transactions)}

        ### REQUIRED SCHEMA (STRICT JSON)
        {{
          "standard_report": "Executive Summary in Markdown",
          "analysis_id": "{str(uuid.uuid4())}",
          "health_score": int,
          "complexity_score": int,
          "model_used": "Gemini 2.0 Flash | Claude 3.5 Sonnet",
          "spending_velocity": float,
          "ghost_subscriptions": ["Service A", "Service B"],
          "tax_strategy": {{
             "deductible_estimate": float,
             "flagged_events": []
          }},
          "requires_expert_followup": boolean,
          "suggestions": ["S1", "S2"]
        }}
        """

        try:
            # STEP 1: Triage with Gemini Flash
            logger.info(f"Quantum Router: Triage pass for user={user_id}")
            triage_prompt = f"Analyze this request complexity (1-10). User query: '{user_query}'. Transactions count: {len(transactions)}. Respond with ONLY an integer."
            triage_res = client.models.generate_content(model=model, contents=triage_prompt)

            try:
                complexity_score = int(triage_res.text.strip())
            except Exception:
                complexity_score = 5

            should_escalate = complexity_score > 7 or (user_query and any(
                kw in user_query.lower() for kw in ["tax", "invest", "strategy", "legal"]
            ))

            used_model = "Gemini 2.0 Flash"
            logger.info(f"Quantum Router: Complexity={complexity_score}. Escalation={should_escalate}.")

            # STEP 2: Main Analysis
            flash_response = client.models.generate_content(
                model=model,
                contents=JASS_SYSTEM_INSTRUCTION + "\n\n" + user_prompt,
                config=types.GenerateContentConfig(response_mime_type="application/json")
            )

            result = self._extract_json(flash_response.text)
            result["complexity_score"] = complexity_score
            result["model_used"] = used_model

            # STEP 3: Route to Claude 3.5 Sonnet if escalated
            if should_escalate:
                try:
                    claude_client = AnthropicVertex(region=self.location, project_id=self.project_id)
                    safe_query = (user_query or "Provide deep investment strategy overview.")[:500]
                    claude_prompt = f"""
                    You are Jass 2.0 (Deep Strategy Mode).
                    Based on this Audit: {json.dumps(result)}
                    <user_question>
                    {safe_query}
                    </user_question>
                    """
                    claude_response = claude_client.messages.create(
                        model="claude-3-5-sonnet@20240620",
                        max_tokens=1024,
                        messages=[{"role": "user", "content": claude_prompt}]
                    )
                    result["expert_advice"] = claude_response.content[0].text
                    result["requires_expert_followup"] = True
                    result["model_used"] = "Claude 3.5 Sonnet"
                except Exception as ce:
                    logger.warning(f"Claude escalation failed, staying with Gemini: {ce}")

            self._log_to_cloud(result)
            return result

        except Exception as e:
            logger.error(f"Hybrid Router failed: {str(e)}")
            return {
                "standard_report": "Jass 2.0 is temporarily offline. Standard wealth tracking remains active.",
                "analysis_id": str(uuid.uuid4()),
                "health_score": 0,
                "complexity_score": 0,
                "model_used": "Fallback Engine"
            }

    def _log_to_cloud(self, result: dict):
        try:
            from google.cloud import logging as cloud_logging
            cloud_client = cloud_logging.Client()
            cloud_client.logger("wealthix_ai_analytics").log_struct({
                "analysis_id": result.get("analysis_id"),
                "model_used": result.get("model_used"),
                "complexity_score": result.get("complexity_score"),
                "health_score": result.get("health_score"),
                "timestamp": datetime.now().isoformat()
            }, severity="INFO")
        except Exception as e:
            logger.warning(f"Cloud Logging failed (non-critical): {e}")

    async def ingest_user_data(self, user_id: str, transactions: List[Dict[str, Any]]):
        logger.info(f"Ingesting {len(transactions)} txs for user {user_id}")
        pass

    async def query_assistant(self, query: str, user_id: str) -> Dict[str, Any]:
        try:
            client = self._get_client()
            prompt = f"""
            You are Jass 2.0, a financial AI assistant.
            Answer this user's financial question concisely and helpfully.
            User question: {query}
            """
            response = client.models.generate_content(model=self._model_name(), contents=prompt)
            return {
                "answer": response.text,
                "suggestions": ["Check my tax opportunities", "Calculate my burn rate"],
                "analysis_metrics": {}
            }
        except Exception as e:
            logger.error(f"query_assistant failed: {e}")
            return {
                "answer": f"I'm analyzing your question about {query}. Please try again shortly.",
                "suggestions": [],
                "analysis_metrics": {}
            }

ai_service = AIService()
