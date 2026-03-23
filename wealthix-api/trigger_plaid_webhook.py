import hmac
import hashlib
import requests
import json

# Configuration (Matches your wealthix-api/.env)
WEBHOOK_URL = "http://localhost:8080/api/webhooks/plaid"
WEBHOOK_SECRET = "changeme-set-from-plaid-dashboard"

# The event we want to simulate
# item_id: find this in your user_bank_connections table (Supabase)
payload = {
    "webhook_type": "TRANSACTIONS",
    "webhook_code": "DEFAULT_UPDATE",
    "item_id": "item_QA_TEST_001",  # Matches MockDataInitializer.MOCK_ITEM_ID
    "new_transactions": 5
}

def trigger_webhook():
    # 1. Convert payload to the canonical JSON string used by Plaid
    # Note: The controller manually reconstructs this, so spacing matters.
    # Our script will match the logic in PlaidWebhookController.buildRawBody.
    body_str = json.dumps(payload, separators=(',', ':'))
    
    # 2. Compute HMAC-SHA256 signature
    signature = hmac.new(
        WEBHOOK_SECRET.encode('utf-8'),
        body_str.encode('utf-8'),
        hashlib.sha256
    ).hexdigest()
    
    print(f"Triggering Webhook at {WEBHOOK_URL}...")
    print(f"Payload: {body_str}")
    print(f"Signature: {signature}")
    
    # 3. Send the POST request
    headers = {
        "Content-Type": "application/json",
        "Plaid-Verification": signature
    }
    
    try:
        response = requests.post(WEBHOOK_URL, headers=headers, data=body_str)
        print(f"Response Status: {response.status_code}")
        print(f"Response Body: {response.text}")
        
        if response.status_code == 200:
            print("\n✅ Webhook Accepted! Check your wealthix-api logs to see the sync start.")
        else:
            print("\n❌ Webhook Rejected. Check if WEBHOOK_SECRET matches your .env exactly.")
            
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    trigger_webhook()
