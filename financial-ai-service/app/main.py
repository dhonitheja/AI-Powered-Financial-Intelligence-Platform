from fastapi import FastAPI
from app.routes import analysis, assistant

# Initialize structured logging
# setup_logging()

app = FastAPI(
    title="Financial AI Service",
    description="Microservice for transaction categorization and fraud detection",
    version="1.0.0"
)

# Include routers
app.include_router(analysis.router)
app.include_router(assistant.router)

@app.get("/health")
async def health_check():
    return {"status": "healthy"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
