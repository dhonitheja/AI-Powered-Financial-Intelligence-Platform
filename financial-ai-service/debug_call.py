import asyncio
from app.services.gemini_service import call_gemini

async def debug_call():
    print("DEBUG: Calling Gemini...")
    reply = await call_gemini("Tell me a short joke")
    if reply:
        print(f"Reply: {reply}")
    else:
        print("Reply was None!")

if __name__ == "__main__":
    import os
    os.environ["PYTHONPATH"] = "."
    asyncio.run(debug_call())
