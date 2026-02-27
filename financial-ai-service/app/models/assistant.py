from pydantic import BaseModel
from typing import List, Optional

class AssistantQuery(BaseModel):
    query: str
    context: Optional[str] = None # For passing transaction summaries or history

class AssistantResponse(BaseModel):
    answer: str
    confidence_score: float = 0.95
    suggestions: List[str] = []
