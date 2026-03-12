"""Tests for /assistant/chat endpoint."""
import os
from unittest.mock import patch, AsyncMock, MagicMock
from fastapi.testclient import TestClient

os.environ.setdefault("INTERNAL_SERVICE_SECRET", "test-secret-123")
os.environ.setdefault("GOOGLE_API_KEY", "fake-key")

from app.main import app

client = TestClient(app, raise_server_exceptions=False)
HEADERS = {"X-Wealthix-Service-Secret": "test-secret-123"}


def test_chat_returns_reply():
    with patch("app.services.gemini_service.call_gemini", new_callable=AsyncMock) as mock_gemini:
        mock_gemini.return_value = "Save 20% of income."

        resp = client.post(
            "/assistant/chat",
            headers=HEADERS,
            json={
                "message": "How can I save money?",
                "session_id": "test-session-1",
                "history": [],
                "financial_context": {"monthly_obligations": 1500},
            },
        )
    assert resp.status_code == 200
    data = resp.json()
    assert "reply" in data
    assert len(data["reply"]) > 0


def test_chat_handles_gemini_failure():
    with patch("app.services.gemini_service.call_gemini", new_callable=AsyncMock) as mock_gemini:
        mock_gemini.return_value = None  # simulates failure / empty response

        resp = client.post(
            "/assistant/chat",
            headers=HEADERS,
            json={"message": "Hello", "session_id": "test-session-2", "history": []},
        )
    # Should return graceful fallback message, not 500
    assert resp.status_code in (200, 503)
    if resp.status_code == 200:
        assert resp.json().get("reply") is not None


def test_no_pii_in_prompt_construction():
    """Verify the prompt sent to Gemini contains no email/account-number PII."""
    captured_prompts = []

    async def capture_call(prompt: str):
        captured_prompts.append(prompt)
        return "Advice here."

    with patch("app.services.gemini_service.call_gemini", side_effect=capture_call):
        client.post(
            "/assistant/chat",
            headers=HEADERS,
            json={
                "message": "Help me",
                "history": [],
                "financial_context": {
                    "monthly_obligations": 1500,
                    "categories": ["RENT", "SUBSCRIPTION"],
                },
            },
        )

    for prompt in captured_prompts:
        # No email addresses in prompt
        import re
        assert not re.search(r"[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}", prompt), \
            f"PII (email) found in prompt: {prompt[:200]}"
        # No long digit sequences (account/card numbers)
        assert not re.search(r"\b\d{10,16}\b", prompt), \
            f"PII (long number) found in prompt: {prompt[:200]}"
