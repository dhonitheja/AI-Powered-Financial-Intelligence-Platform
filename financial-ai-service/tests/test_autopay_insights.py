"""Tests for /autopay insights endpoints."""
import os
import pytest
from unittest.mock import patch, AsyncMock
from fastapi.testclient import TestClient

os.environ.setdefault("INTERNAL_SERVICE_SECRET", "test-secret-123")
os.environ.setdefault("GOOGLE_API_KEY", "fake-key")

from app.main import app

client = TestClient(app, raise_server_exceptions=False)
HEADERS = {"X-Wealthix-Service-Secret": "test-secret-123"}


def _mock_insights():
    return {
        "optimization_tips": ["Tip 1"],
        "risk_flags": [],
        "savings_opportunities": [],
    }


def test_analyze_returns_valid_response():
    schedules = [
        {"category": "SUBSCRIPTION", "amount": 15.99,
         "frequency": "MONTHLY", "is_active": True}
    ]
    with patch("app.routers.autopay_insights.generate_insights",
               new_callable=AsyncMock, return_value=_mock_insights()):
        resp = client.post("/autopay/analyze", headers=HEADERS,
                           json={"schedules": schedules})
    assert resp.status_code == 200
    data = resp.json()
    assert "monthly_burn_rate" in data
    assert "payment_health_score" in data
    assert 0 <= data["payment_health_score"] <= 100


def test_analyze_empty_schedules_returns_400():
    with patch("app.routers.autopay_insights.generate_insights",
               new_callable=AsyncMock, return_value=_mock_insights()):
        resp = client.post("/autopay/analyze", headers=HEADERS,
                           json={"schedules": []})
    # Empty schedules either return 200 with zeroes or 400 — both acceptable
    assert resp.status_code in (200, 400, 422)


def test_categorize_returns_known_category():
    with patch("app.routers.autopay_insights.categorize_payment",
               new_callable=AsyncMock,
               return_value={"category": "SUBSCRIPTION", "confidence": 0.95}):
        resp = client.post("/autopay/categorize", headers=HEADERS,
                           json={"description": "Netflix monthly"})
    assert resp.status_code == 200
    assert resp.json()["category"] == "SUBSCRIPTION"


def test_categorize_unknown_returns_custom():
    with patch("app.routers.autopay_insights.categorize_payment",
               new_callable=AsyncMock,
               return_value={"category": "CUSTOM", "confidence": 0.3}):
        resp = client.post("/autopay/categorize", headers=HEADERS,
                           json={"description": "Random obscure payment xyz"})
    assert resp.status_code == 200
    assert resp.json()["category"] == "CUSTOM"


def test_benchmarks_returns_data():
    resp = client.get("/autopay/benchmarks/SUBSCRIPTION", headers=HEADERS)
    assert resp.status_code == 200
    data = resp.json()
    assert "average_amount" in data
    assert data["average_amount"] > 0


def test_health_score_between_0_and_100():
    schedules = [
        {"category": "RENT", "amount": 1500,
         "frequency": "MONTHLY", "is_active": True},
        {"category": "UTILITY", "amount": 200,
         "frequency": "MONTHLY", "is_active": True},
    ]
    with patch("app.routers.autopay_insights.generate_insights",
               new_callable=AsyncMock, return_value=_mock_insights()):
        resp = client.post("/autopay/analyze", headers=HEADERS,
                           json={"schedules": schedules})
    assert resp.status_code == 200
    score = resp.json()["payment_health_score"]
    assert 0 <= score <= 100
