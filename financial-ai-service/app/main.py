from fastapi import FastAPI, Request, status
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager
import google.generativeai as genai
import os
import uvicorn
import logging
import json
import sys
from datetime import datetime

from app.routers import (
    autopay_insights, 
    transaction_enrichment, 
    financial_advice,
    assistant,
    anomaly_detection,
    spending_forecast,
    budget_recommendations
)


class WealthixJsonFormatter(logging.Formatter):
    def format(self, record):
        log_entry = {
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "level": record.levelname,
            "service": "wealthix-ai",
            "message": record.getMessage(),
            "logger": record.name,
        }
        # NEVER log request bodies or response data
        # that might contain financial information
        return json.dumps(log_entry)

handler = logging.StreamHandler(sys.stdout)
handler.setFormatter(WealthixJsonFormatter())
logging.basicConfig(level=logging.INFO, handlers=[handler])

# Initialize Gemini once at startup — not per request
@asynccontextmanager
async def lifespan(app: FastAPI):
    genai.configure(api_key=os.getenv("GOOGLE_API_KEY"))
    logging.info("Gemini initialized")
    yield
    logging.info("Shutting down")

app = FastAPI(
    title="Wealthix AI Service",
    description="Internal AI service for Wealthix platform",
    version="1.0.0",
    docs_url=None,      # disable public Swagger
    redoc_url=None,     # disable public ReDoc
    lifespan=lifespan
)

# Request logging middleware (logs method + path ONLY, no bodies)
@app.middleware("http")
async def log_requests(request: Request, call_next):
    start = datetime.utcnow()
    response = await call_next(request)
    duration = (datetime.utcnow() - start).total_seconds()
    logging.info(json.dumps({
        "method": request.method,
        "path": request.url.path,
        # Never log: query params, headers, body
        "status": response.status_code,
        "duration_ms": round(duration * 1000, 2),
        "service": "wealthix-ai"
    }))
    return response

app.include_router(autopay_insights.router, prefix="/autopay")
app.include_router(transaction_enrichment.router, prefix="/transactions")
app.include_router(financial_advice.router, prefix="/advice")
app.include_router(assistant.router, prefix="/assistant")
app.include_router(anomaly_detection.router, prefix="/anomaly")
app.include_router(spending_forecast.router, prefix="/forecast")
app.include_router(budget_recommendations.router, prefix="/budget")

@app.get("/health")
async def health():
    return {"status": "ok", "service": "wealthix-ai"}

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=int(os.getenv("PORT", 8000)), reload=True)
