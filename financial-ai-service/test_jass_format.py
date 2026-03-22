import asyncio
import json
from app.services.ai_service import ai_service

async def test_jass():
    query = "Analyze my spending velocity and risk score based on recent transactions."
    # Mocking user_id. In a real scenario, RAG would provide context.
    user_id = "test-user-123"
    
    print(f"Testing Jass with query: {query}")
    try:
        result = await ai_service.query_assistant(query, user_id=user_id)
        print("\nJass Response:")
        print(json.dumps(result, indent=2))
        
        # Verify required fields from Jass instructions
        required_fields = ["answer", "analysis_metrics", "suggestions"]
        for field in required_fields:
            if field not in result:
                print(f"MISSING FIELD: {field}")
            else:
                print(f"FOUND FIELD: {field}")

        # Verify internal metrics structure
        metrics = result.get("analysis_metrics", {})
        metric_fields = ["spending_velocity_label", "risk_score", "wealth_potential_score", "ghost_subscriptions_found"]
        for mf in metric_fields:
            if mf not in metrics:
                print(f"MISSING METRIC: {mf}")
            else:
                print(f"FOUND METRIC: {mf}")

    except Exception as e:
        print(f"TEST FAILED: {str(e)}")

if __name__ == "__main__":
    asyncio.run(test_jass())
