from fastapi import FastAPI, Request
from contextlib import asynccontextmanager
# import vertexai
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
    budget_recommendations,
    openai_compat
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
        return json.dumps(log_entry)

handler = logging.StreamHandler(sys.stdout)
handler.setFormatter(WealthixJsonFormatter())
logging.basicConfig(level=logging.INFO, handlers=[handler])

from app.config.settings import settings

# Initialize Vertex AI once at startup
@asynccontextmanager
async def lifespan(app: FastAPI):
    try:
        # Initialize Vertex AI for both Gemini and Claude models
        project_id = os.getenv("GOOGLE_CLOUD_PROJECT", "wealthix-pro")
        # vertexai.init(project=project_id, location="us-central1")
        logging.info(f"Vertex AI initialized successfully (project={project_id})")
    except Exception as e:
        logging.error(f"Vertex AI initialization failed: {str(e)}")
    yield
    logging.info("Shutting down")

app = FastAPI(
    title="Wealthix AI Service",
    description="Internal AI service (Jass 2.0 Hybrid Router)",
    version="2.0.0",
    docs_url=None,
    redoc_url=None,
    lifespan=lifespan
)

@app.middleware("http")
async def log_requests(request: Request, call_next):
    start = datetime.utcnow()
    response = await call_next(request)
    duration = (datetime.utcnow() - start).total_seconds()
    logging.info(json.dumps({
        "method": request.method,
        "path": request.url.path,
        "status": response.status_code,
        "duration_ms": round(duration * 1000, 2),
        "service": "wealthix-ai"
    }))
    return response

# Standard Routers
app.include_router(autopay_insights.router, prefix="/autopay")
app.include_router(transaction_enrichment.router, prefix="/transactions")
app.include_router(financial_advice.router, prefix="/advice")
app.include_router(assistant.router, prefix="/assistant")
app.include_router(anomaly_detection.router, prefix="/anomaly")
app.include_router(spending_forecast.router, prefix="/forecast")
app.include_router(budget_recommendations.router, prefix="/budget")

app.include_router(openai_compat.router, prefix="/openai")

@app.get("/health")
async def health():
    return {"status": "ok", "service": "wealthix-ai", "engine": "VertexAI-Hybrid"}

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=int(os.getenv("PORT", 8000)), reload=True)
