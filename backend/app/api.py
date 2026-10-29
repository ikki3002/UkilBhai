from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from app.retriever import LegalRetriever
from app.generator import generate_legal_advice

router = APIRouter()

class RetrieveRequest(BaseModel):
    query: str = Field(..., description="Citizen legal query or section name in Bengali or English")
    top_k: int = Field(default=5, ge=1, le=20, description="Number of legal sections to retrieve")

class ChatHistoryMessage(BaseModel):
    role: str
    content: str

class AskRequest(BaseModel):
    query: str = Field(..., description="Citizen's legal issue or question")
    history: Optional[List[ChatHistoryMessage]] = Field(default=None, description="Previous messages in chat")
    top_k: int = Field(default=4, ge=1, le=10, description="Sections to inject into RAG context")

@router.get("/")
def home():
    retriever = LegalRetriever.get_instance()
    return {
        "service": "Ukil Bhai Legal RAG Service",
        "description": "AI-powered legal consultation backend for Bangladesh statutory laws",
        "dataset": "sakhadib/Bangladesh-Legal-Acts-Dataset",
        "status": "ready",
        "indexed_sections": len(retriever.sections),
        "docs_url": "/docs"
    }

@router.get("/health")
def health():
    retriever = LegalRetriever.get_instance()
    return {
        "status": "healthy",
        "sections_indexed": len(retriever.sections),
        "bm25_ready": retriever.bm25 is not None
    }

@router.post("/retrieve")
def retrieve_statutes(req: RetrieveRequest):
    if not req.query.strip():
        raise HTTPException(status_code=400, detail="Query cannot be empty")
    retriever = LegalRetriever.get_instance()
    results = retriever.search(req.query.strip(), top_k=req.top_k)
    return {
        "query": req.query,
        "count": len(results),
        "results": results
    }

@router.post("/ask")
def ask_legal_ai(req: AskRequest):
    if not req.query.strip():
        raise HTTPException(status_code=400, detail="Query cannot be empty")

    retriever = LegalRetriever.get_instance()
    sections = retriever.search(req.query.strip(), top_k=req.top_k)

    history_dicts = None
    if req.history:
        history_dicts = [{"role": m.role, "content": m.content} for m in req.history]

    answer, provider, citations = generate_legal_advice(
        query=req.query.strip(),
        sections=sections,
        history=history_dicts
    )

    return {
        "query": req.query,
        "answer": answer,
        "provider": provider,
        "citations": citations
    }
