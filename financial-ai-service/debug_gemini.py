import google.generativeai as genai

api_key = "AIzaSyCFPPPuAvu2-Re4CHyyQC9cVHgOmcMX82M"
genai.configure(api_key=api_key)
model = genai.GenerativeModel("gemini-pro")

try:
    response = model.generate_content("Hello")
    print(f"Response: {response.text}")
except Exception as e:
    print(f"Error: {e}")
