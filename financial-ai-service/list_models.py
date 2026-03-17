import google.generativeai as genai

api_key = "AIzaSyCFPPPuAvu2-Re4CHyyQC9cVHgOmcMX82M"
genai.configure(api_key=api_key)

try:
    for m in genai.list_models():
        if 'generateContent' in m.supported_generation_methods:
            print(m.name)
except Exception as e:
    print(f"Error: {e}")
