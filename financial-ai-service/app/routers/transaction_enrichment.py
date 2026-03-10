from fastapi import APIRouter, Depends
from app.middleware.auth_middleware import verify_internal_secret
from app.models.transaction_models import EnrichmentRequest, EnrichmentResponse

router = APIRouter(
    dependencies=[Depends(verify_internal_secret)])

@router.post("/enrich", response_model=EnrichmentResponse)
async def enrich_transactions(request: EnrichmentRequest):
    """
    Enrich raw transactions with smart descriptions and normalized categories.
    Placeholder until full implementation.
    """
    # Simple placeholder logic to return dummy data until actual AI integration
    enriched_list = []
    for t in request.transactions:
        enriched_list.append({
            "original_description": t.description,
            "smart_description": t.description.strip().title(),
            "normalized_category": t.raw_category.upper(),
            "confidence": 0.5
        })
    return EnrichmentResponse(enriched=enriched_list, processed_count=len(enriched_list))
