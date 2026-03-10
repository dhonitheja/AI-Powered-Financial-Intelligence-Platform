import os
from fastapi import Request, HTTPException, status
from dotenv import load_dotenv

load_dotenv()
INTERNAL_SECRET = os.getenv("INTERNAL_SERVICE_SECRET")

async def verify_internal_secret(request: Request):
    """
    Verify every request comes from wealthix-api only.
    Reject anything without the correct shared secret header.
    NEVER expose this service publicly.
    """
    secret = request.headers.get("X-Wealthix-Service-Secret")
    if not secret or secret != INTERNAL_SECRET:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Unauthorized: Invalid service secret"
        )
