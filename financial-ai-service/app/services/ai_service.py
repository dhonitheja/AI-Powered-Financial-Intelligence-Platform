import vertexai
from vertexai.generative_models import GenerativeModel, Part, GenerationConfig
from anthropic import AnthropicVertex
import json
import logging
from typing import List, Dict, Any, Optional
import uuid
from datetime import datetime

# Configure Local Logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("wealthix-ai")

# Jass 2.0 Persona Constants
JASS_SYSTEM_INSTRUCTION = """
You are Jass 2.0, a Quantum-Level Financial Intelligence Engine.
Your analysis architecture combines Big Four auditing precision with VC strategic foresight.
Focus on: Ghost Subscription detection, Spending Velocity, and Tax Strategy.
"""

class AIService:
    def __init__(self):
        self.project_id = "wealthix-pro"
        self.location = "us-central1"
        vertexai.init(project=self.project_id, location=self.location)

    def _extract_json(self, response_text: str) -> dict:
        try:
            # Clean up potential markdown formatting
            clean_text = response_text.replace("```json", "").replace("```", "").strip()
            return json.loads(clean_text)
        except Exception as e:
            logger.error(f"Failed to parse AI JSON: {e}")
            return {"error": "Parsing error"}

    # Synchronous — Vertex AI SDK calls are blocking; wrapping them as async
    # without awaiting blocks the event loop. Use run_in_executor if async is needed.
    def analyze_finances(self, user_id: str, transactions: List[Dict[str, Any]], user_query: Optional[str] = None):
        flash_model = GenerativeModel("gemini-1.5-flash")

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
          "model_used": "Gemini 1.5 Flash | Claude 3.5 Sonnet",
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
            # STEP 1: High-Speed Triage with Gemini Flash
            logger.info(f"Quantum Router: Triage pass for user={user_id}")
            triage_prompt = f"Analyze this request complexity (1-10). User query: '{user_query}'. Transactions count: {len(transactions)}. Respond with ONLY an integer."
            triage_res = flash_model.generate_content(triage_prompt)

            try:
                complexity_score = int(triage_res.text.strip())
            except Exception:
                complexity_score = 5  # Default

            # Escalation Rule: Score > 7 OR expert keywords
            should_escalate = complexity_score > 7 or (user_query and any(kw in user_query.lower() for kw in ["tax", "invest", "strategy", "legal"]))

            model_name = "Gemini 1.5 Flash"
            if should_escalate:
                model_name = "Claude 3.5 Sonnet"

            logger.info(f"Quantum Router: Complexity={complexity_score}. Escalation={should_escalate}. Using {model_name}")

            # STEP 2: Main Analysis
            flash_response = flash_model.generate_content(
                JASS_SYSTEM_INSTRUCTION + "\n\n" + user_prompt,
                generation_config=GenerationConfig(response_mime_type="application/json")
            )

            result = self._extract_json(flash_response.text)
            result["complexity_score"] = complexity_score
            result["model_used"] = "Gemini 1.5 Flash"

            # STEP 3: Route to Claude 3.5 Sonnet if escalated
            if should_escalate:
                client = AnthropicVertex(region=self.location, project_id=self.project_id)

                # Sanitize user_query before interpolating into prompt
                safe_query = (user_query or "Provide deep investment strategy overview.")[:500]

                claude_prompt = f"""
                You are Jass 2.0 (Deep Strategy Mode).
                Based on this Audit: {json.dumps(result)}
                <user_question>
                {safe_query}
                </user_question>
                """

                claude_response = client.messages.create(
                    model="claude-3-5-sonnet@20240620",
                    max_tokens=1024,
                    messages=[{"role": "user", "content": claude_prompt}]
                )

                result["expert_advice"] = claude_response.content[0].text
                result["requires_expert_followup"] = True
                result["model_used"] = "Claude 3.5 Sonnet"

            # STEP 4: Cloud Logging for Analytics Dashboard
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
        """
        Sends metadata to Google Cloud Logging for the analytics dashboard.
        """
        try:
            from google.cloud import logging as cloud_logging
            client = cloud_logging.Client()
            logger_cloud = client.logger("wealthix_ai_analytics")

            logger_cloud.log_struct({
                "analysis_id": result.get("analysis_id"),
                "model_used": result.get("model_used"),
                "complexity_score": result.get("complexity_score"),
                "health_score": result.get("health_score"),
                "timestamp": datetime.now().isoformat()
            }, severity="INFO")
            logger.info(f"Audit Log Sent to Cloud: {result.get('analysis_id')}")
        except Exception as e:
            logger.warning(f"Cloud Logging failed: {e}")

    async def ingest_user_data(self, user_id: str, transactions: List[Dict[str, Any]]):
        logger.info(f"Ingesting {len(transactions)} txs for user {user_id} into Vertex RAG")
        pass

    async def query_assistant(self, query: str, user_id: str) -> Dict[str, Any]:
        return {
            "answer": f"I see your query about {query}. I'm analyzing your historical patterns across Vertex AI engines.",
            "suggestions": ["Check my tax opportunities", "Calculate my burn rate"],
            "analysis_metrics": {}  # Required by ChatResponse model
        }

ai_service = AIService()
