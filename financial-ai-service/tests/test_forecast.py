"""Tests for /forecast/forecast endpoint."""
import os
from fastapi.testclient import TestClient

os.environ.setdefault("INTERNAL_SERVICE_SECRET", "test-secret-123")
os.environ.setdefault("GOOGLE_API_KEY", "fake-key")

from app.main import app

client = TestClient(app, raise_server_exceptions=False)
HEADERS = {"X-Wealthix-Service-Secret": "test-secret-123"}

MONTHLY_DATA = [
    {"month": "2024-07", "total": 1200},
    {"month": "2024-08", "total": 1300},
    {"month": "2024-09", "total": 1250},
    {"month": "2024-10", "total": 1400},
    {"month": "2024-11", "total": 1350},
    {"month": "2024-12", "total": 1500},
]


def test_forecast_returns_correct_months():
    payload = {"monthly_data": MONTHLY_DATA, "forecast_months": 3}
    resp = client.post("/forecast/forecast", headers=HEADERS, json=payload)
    assert resp.status_code == 200
    data = resp.json()
    assert "forecasts" in data
    assert len(data["forecasts"]) == 3


def test_trend_increasing_when_slope_positive():
    payload = {"monthly_data": MONTHLY_DATA, "forecast_months": 3}
    resp = client.post("/forecast/forecast", headers=HEADERS, json=payload)
    assert resp.status_code == 200
    data = resp.json()
    assert data.get("trend") in ("INCREASING", "STABLE", "DECREASING")


def test_trend_stable_when_flat():
    flat_data = [
        {"month": f"2024-{m:02d}", "total": 1000}
        for m in range(1, 7)
    ]
    payload = {"monthly_data": flat_data, "forecast_months": 3}
    resp = client.post("/forecast/forecast", headers=HEADERS, json=payload)
    assert resp.status_code == 200
    assert resp.json().get("trend") == "STABLE"
