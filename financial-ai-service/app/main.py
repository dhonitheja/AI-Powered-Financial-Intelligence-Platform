from fastapi import FastAPI, Request, status
from fastapi.responses import JSONResponse
from app.routers import autopay_insights, transaction_enrichment, financial_advice
import os
import uvicorn

app = FastAPI(
    title="Wealthix AI Service",
    description="Internal AI service for Wealthix platform",
    version="1.0.0",
    docs_url=None,      # disable public Swagger
    redoc_url=None      # disable public ReDoc
)

app.include_router(autopay_insights.router, prefix="/autopay")
app.include_router(
    transaction_enrichment.router, prefix="/transactions")
app.include_router(financial_advice.router, prefix="/advice")

@app.get("/health")
async def health():
    return {"status": "ok", "service": "wealthix-ai"}

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=int(os.getenv("PORT", 8000)), reload=True)
