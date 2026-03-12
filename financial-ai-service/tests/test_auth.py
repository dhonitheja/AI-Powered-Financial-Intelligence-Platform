"""Tests for internal service authentication middleware."""
import os
import pytest
from unittest.mock import patch, AsyncMock
from fastapi.testclient import TestClient

# Set env vars before importing app
os.environ.setdefault("INTERNAL_SERVICE_SECRET", "test-secret-123")
os.environ.setdefault("GOOGLE_API_KEY", "fake-key")

from app.main import app

client = TestClient(app, raise_server_exceptions=False)
VALID_SECRET = "test-secret-123"


def test_missing_secret_returns_403():
    resp = client.post("/autopay/analyze", json={"schedules": []})
    assert resp.status_code == 403


def test_wrong_secret_returns_403():
    resp = client.post(
        "/autopay/analyze",
        headers={"X-Wealthix-Service-Secret": "wrong-secret"},
        json={"schedules": []},
    )
    assert resp.status_code == 403


def test_correct_secret_passes():
    with patch("app.routers.autopay_insights.generate_insights", new_callable=AsyncMock) as mock_gi, \
         patch("app.routers.autopay_insights.categorize_payment", new_callable=AsyncMock):
        mock_gi.return_value = {
            "optimization_tips": [],
            "risk_flags": [],
            "savings_opportunities": [],
        }
        resp = client.post(
            "/autopay/analyze",
            headers={"X-Wealthix-Service-Secret": VALID_SECRET},
            json={"schedules": []},
        )
    # Request reaches the handler (not rejected by auth)
    assert resp.status_code != 403
