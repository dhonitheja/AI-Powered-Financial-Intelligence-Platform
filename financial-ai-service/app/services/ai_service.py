import logging
import json
import google.generativeai as genai
from app.models.transaction import TransactionInput
from app.config.settings import settings
from fastapi import HTTPException

logger = logging.getLogger(__name__)

class AIService:
    def __init__(self):
        self._model = None

    def _get_model(self):
        if self._model is None:
            if not settings.gemini_api_key:
                logger.error("GEMINI_API_KEY environment variable is not set")
                raise ValueError("GEMINI_API_KEY must be set to use AI features")
            
            genai.configure(api_key=settings.gemini_api_key)
            # Using generation_config to encourage JSON output if the model supports it, 
            # though standard prompt constraints are still very effective.
            self._model = genai.GenerativeModel(
                model_name=settings.model_name,
                generation_config={"response_mime_type": "application/json"}
            )
        return self._model

    async def analyze_transaction(self, transaction: TransactionInput) -> dict:
        """
        Analyzes a transaction using Gemini 1.5 Flash to determine category and fraud risk.
        Returns a structured JSON response with category, fraudRiskScore, and explanation.
        """
        model = self._get_model()
        
        prompt = f"""
        Analyze the following financial transaction for categorization and fraud detection.
        
        Transaction Details:
        - Description: {transaction.description}
        - Amount: {transaction.amount}
        - Date: {transaction.transactionDate}
        
        Context:
        - Category: High-level financial categories (e.g., Food, Travel, Utilities, Groceries, etc.).
        - Fraud Risk Score: A float between 0.0 and 1.0 (0.0 = safe, 1.0 = highly suspicious).
        - Explanation: Brief reasoning for the category and fraud score.
        
        Strictly return ONLY a JSON object in this format:
        {{
          "category": "string",
          "fraudRiskScore": float,
          "explanation": "string"
        }}
        """

        try:
            logger.info(f"Sending analysis request to Gemini ({settings.model_name}) for: {transaction.description}")
            response = model.generate_content(prompt)
            
            # Extract and parse JSON
            text_response = response.text.strip()
            # Handle potential markdown wrapper if model ignores 'RAW JSON' instruction
            if text_response.startswith("```"):
                text_response = text_response.strip("`").strip("json").strip()
            
            result = json.loads(text_response)
            
            # Basic validation of schema
            required_keys = ["category", "fraudRiskScore", "explanation"]
            if not all(k in result for k in required_keys):
                logger.error(f"Incomplete AI response: {result}")
                raise ValueError("AI response missing required fields")

            logger.info(f"Successful AI analysis for: {transaction.description}")
            return result

        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse AI response as JSON: {response.text}")
            raise HTTPException(status_code=500, detail="AI returned invalid JSON format")
    async def query_assistant(self, query: str, context: str = None) -> dict:
        """
        Handles general financial assistant queries using Gemini.
        Returns context-aware financial advice and summaries.
        """
        model = self._get_model()
        
        system_prompt = """
        You are a highly intelligent Personal Finance Assistant named Antigravity. 
        Your goal is to provide proactive financial insights, spending analysis, 
        and budget recommendations based on the user's data.

        Guidelines:
        - Be concise, professional, and encouraging.
        - If financial data (context) is provided, use it to give specific advice.
        - Suggest 3 follow-up actions or questions for the user.
        - Provide a confidence score (0.0 to 1.0) for your analysis based on data availability.
        - Do not provide actual investment advice (stocks, crypto picks); focus on budgeting and spending habits.
        
        Strictly return ONLY a JSON object in this format:
        {
          "answer": "string (markdown supported)",
          "confidence_score": float,
          "suggestions": ["suggestion 1", "suggestion 2", "suggestion 3"]
        }
        """

        user_prompt = f"User Question: {query}\n\nFinancial Context (Transactions/Summary): {context or 'No data provided.'}"
        
        try:
            logger.info(f"Sending assistant query to Gemini: {query[:50]}...")
            response = model.generate_content(system_prompt + "\n" + user_prompt)
            
            text_response = response.text.strip()
            if text_response.startswith("```"):
                text_response = text_response.strip("`").strip("json").strip()
            
            result = json.loads(text_response)
            # Ensure confidence_score exists
            if "confidence_score" not in result:
                result["confidence_score"] = 0.9
            return result

        except Exception as e:
            logger.error(f"Error in Assistant Query: {str(e)}")
            return {
                "answer": "I'm sorry, I'm having trouble processing your request right now. Could you try again?",
                "confidence_score": 0.0,
                "suggestions": ["Tell me about my spending", "What is my fraud risk?", "How can I save more?"]
            }

# Singleton instance
ai_service = AIService()
