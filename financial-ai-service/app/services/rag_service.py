import os
import logging
import numpy as np
import pandas as pd
from typing import List, Dict, Any
from langchain_google_genai import GoogleGenerativeAIEmbeddings
from langchain_community.vectorstores import FAISS
from app.config.settings import settings

logger = logging.getLogger(__name__)

class RAGService:
    def __init__(self):
        self._embeddings = None
        self._vector_store = None
        self._index_path = "faiss_index"

    def _get_embeddings(self):
        if self._embeddings is None:
            self._embeddings = GoogleGenerativeAIEmbeddings(
                model="models/embedding-001",
                google_api_key=settings.gemini_api_key
            )
        return self._embeddings

    def ingest_transactions(self, transactions: List[Dict[str, Any]]):
        """
        Ingests transactions into the vector store.
        Each transaction is converted to a descriptive text string.
        """
        if not transactions:
            logger.info("No transactions to ingest.")
            return

        texts = []
        metadatas = []
        
        for tx in transactions:
            # Format: "On [date], spent [amount] at [description] (Category: [category], Account: [accountSubtype])"
            text = f"On {tx.get('date')}, spent {tx.get('amount')} at {tx.get('description')} (Category: {tx.get('category')}, Account: {tx.get('accountSubtype')})"
            texts.append(text)
            metadatas.append(tx)

        embeddings = self._get_embeddings()
        self._vector_store = FAISS.from_texts(texts, embeddings, metadatas=metadatas)
        
        # Optionally save to disk
        # self._vector_store.save_local(self._index_path)
        logger.info(f"Ingested {len(transactions)} transactions into FAISS.")

    def search_context(self, query: str, k: int = 10) -> str:
        """
        Search for relevant context for a query.
        """
        if self._vector_store is None:
            return "No historical transaction context available."

        try:
            docs = self._vector_store.similarity_search(query, k=k)
            context = "\n".join([doc.page_content for doc in docs])
            return context
        except Exception as e:
            logger.error(f"Error searching vector store: {str(e)}")
            return "Error retrieving historical context."

rag_service = RAGService()
