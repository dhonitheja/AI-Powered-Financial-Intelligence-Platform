import pytest
import json
import os
from fastapi.testclient import TestClient
from app.main import app

def load_mock_txs():
    mock_path = os.path.join(os.path.dirname(__file__), "ghost_test_data.json")
    with open(mock_path, "r") as f:
        return json.load(f)

def test_ghost_subscription_detection():
    """
    Test that Jass detects complex noisy subscriptions (Amazon, Geico, Hulu).
    """
    with TestClient(app) as client:
        transactions = load_mock_txs()
        
        # Internal secret required by middleware
        headers = {
            "X-Wealthix-Service-Secret": os.getenv("WEALTHIX_INTERNAL_SECRET", "dev_internal_secret_key_123"),
            "Content-Type": "application/json"
        }
        
        payload = {
            "user_id": "test-user-uuid",
            "transactions": transactions
        }
        
        # Add a mock tax query to trigger Claude 3.5 Sonnet on Vertex
        payload["user_query"] = "What is my total tax-deductible estimate for this period, and how should I report the HSA transfer?"
        
        print("[Test] Sending 180-day data with TAX QUERY to Hybrid Router...")
        response = client.post("/assistant/analyze", json=payload, headers=headers)
        
        assert response.status_code == 200
        try:
            data = response.json()
        except Exception as e:
            print(f"FAILED TO PARSE JSON! Raw Response: {response.text}")
            raise e
        
        # 1. Assert Gemini Flash Metrics
        metrics = data.get("analysis_metrics", {})
        ghost_count = metrics.get("ghost_subscriptions_found", 0)
        print(f"[Test] Gemini Flash found {ghost_count} ghost subscriptions.")
        assert ghost_count > 0 
        
        # 2. Assert Claude 3.5 Sonnet Expert Advice
        expert_advice = data.get("expert_advice")
        print(f"[Test] Claude 3.5 Sonnet Expert Advice: {expert_advice}")
        assert expert_advice is not None, "Hybrid Router failed to trigger Claude for tax query."
        assert "HSA" in expert_advice or "tax" in expert_advice.lower()

        # 3. Assert Financial Health Score
        health_score = data.get("financial_health_score", 0)
        print(f"[Test] Financial health score: {health_score}")
        assert health_score > 0

        # 4. Assert analysis_id present
        assert "analysis_id" in data

if __name__ == "__main__":
    test_ghost_subscription_detection()
    print("Test finished manually.")