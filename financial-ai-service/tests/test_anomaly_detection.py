"""Tests for /anomaly/detect endpoint."""
import os
import pytest
from fastapi.testclient import TestClient

os.environ.setdefault("INTERNAL_SERVICE_SECRET", "test-secret-123")
os.environ.setdefault("GOOGLE_API_KEY", "fake-key")

from app.main import app

client = TestClient(app, raise_server_exceptions=False)
HEADERS = {"X-Wealthix-Service-Secret": "test-secret-123"}


def test_amount_spike_detected():
    payload = {
        "schedules": [{"category": "SUBSCRIPTION", "amount": 300}],
        "historical_averages": {"SUBSCRIPTION": 15},
    }
    resp = client.post("/anomaly/detect", headers=HEADERS, json=payload)
    assert resp.status_code == 200
    data = resp.json()
    anomaly_types = [a["type"] for a in data["anomalies"]]
    assert "AMOUNT_SPIKE" in anomaly_types


def test_concentration_risk_detected():
    payload = {
        "schedules": [
            {"category": "RENT", "amount": 2000},
            {"category": "SUBSCRIPTION", "amount": 10},
        ],
        "historical_averages": {},
    }
    resp = client.post("/anomaly/detect", headers=HEADERS, json=payload)
    assert resp.status_code == 200
    anomaly_types = [a["type"] for a in resp.json()["anomalies"]]
    assert "CONCENTRATION_RISK" in anomaly_types


def test_no_anomaly_for_normal_data():
    payload = {
        "schedules": [
            {"category": "SUBSCRIPTION", "amount": 15},
            {"category": "UTILITY", "amount": 100},
            {"category": "RENT", "amount": 1200},
        ],
        "historical_averages": {
            "SUBSCRIPTION": 14, "UTILITY": 95, "RENT": 1150
        },
    }
    resp = client.post("/anomaly/detect", headers=HEADERS, json=payload)
    assert resp.status_code == 200
    data = resp.json()
    spike_anomalies = [a for a in data["anomalies"] if a["type"] == "AMOUNT_SPIKE"]
    assert len(spike_anomalies) == 0
