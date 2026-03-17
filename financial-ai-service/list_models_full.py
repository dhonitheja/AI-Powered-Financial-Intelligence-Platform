import google.generativeai as genai

api_key = "AIzaSyCFPPPuAvu2-Re4CHyyQC9cVHgOmcMX82M"
genai.configure(api_key=api_key)

try:
    print("Listing all models...")
    models = list(genai.list_models())
    for m in models:
        print(f"Name: {m.name}, Methods: {m.supported_generation_methods}")
except Exception as e:
    print(f"Error: {e}")
