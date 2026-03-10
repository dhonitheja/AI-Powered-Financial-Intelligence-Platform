from pydantic import BaseModel, Field
from typing import List

class TransactionInput(BaseModel):
    """Anonymized transaction data — NO names, NO account numbers."""
    description:    str   = Field(min_length=1, max_length=500)
    amount:         float = Field(gt=0)
    raw_category:   str   = Field(default="UNKNOWN", max_length=100)

class EnrichmentRequest(BaseModel):
    transactions: List[TransactionInput] = Field(
        min_length=1, max_length=100)

class EnrichedTransaction(BaseModel):
    original_description: str
    smart_description:    str
    normalized_category:  str
    confidence:           float = Field(ge=0.0, le=1.0)

class EnrichmentResponse(BaseModel):
    enriched: List[EnrichedTransaction]
    processed_count: int
