import requests
import json

def test_chat():
    url = "http://localhost:8000/assistant/chat"
    payload = {
        "message": "What is my fraud risk for a $500 Starbucks transaction?",
        "session_id": "test-session",
        "history": [],
        "financial_context": {"groceries": 300.0, "starbucks": 5.0}
    }
    headers = {
        "Content-Type": "application/json",
        "X-Wealthix-Service-Secret": "dev_internal_secret_key_123"
    }
    
    try:
        print(f"Calling: {url}")
        response = requests.post(url, data=json.dumps(payload), headers=headers)
        print(f"Status: {response.status_code}")
        if response.status_code == 200:
            print(f"Response: {json.dumps(response.json(), indent=2)}")
        else:
            print(f"Error Body: {response.text}")
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    test_chat()
