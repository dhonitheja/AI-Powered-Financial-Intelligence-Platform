import requests
import json
import time

URL = "http://127.0.0.1:8000/assistant/analyze"
SECRET = "dev_internal_secret_key_123"

def run_volume_test(iterations=30):
    print(f"Starting Volume Test: {iterations} requests...")
    success_count = 0
    fail_count = 0
    
    headers = {
        "X-Wealthix-Service-Secret": SECRET,
        "Content-Type": "application/json"
    }
    
    data = {
        "user_id": "test-user-volume",
        "transactions": [
            {"amount": 100.0, "description": "Starbucks", "date": "2025-01-01"},
            {"amount": 15.99, "description": "Netflix", "date": "2025-01-02"}
        ],
        "user_query": "Give me a quick summary of my small spending."
    }
    
    for i in range(iterations):
        try:
            start_time = time.time()
            response = requests.post(URL, headers=headers, json=data, timeout=30)
            duration = time.time() - start_time
            
            if response.status_code == 200:
                success_count += 1
                print(f"[{i+1}/{iterations}] SUCCESS ({duration:.2f}s)")
            else:
                fail_count += 1
                print(f"[{i+1}/{iterations}] FAILED: Status {response.status_code} - {response.text}")
                if "quota" in response.text.lower() or response.status_code == 429:
                    print("!!! QUOTA LIMIT REACHED !!!")
                    break
        except Exception as e:
            fail_count += 1
            print(f"[{i+1}/{iterations}] ERROR: {str(e)}")
            
    print("\n--- TEST SUMMARY ---")
    print(f"Total Requests: {iterations}")
    print(f"Successes: {success_count}")
    print(f"Failures: {fail_count}")

if __name__ == "__main__":
    run_volume_test()
