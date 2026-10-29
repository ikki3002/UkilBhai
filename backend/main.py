from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from app.api import router
from app.retriever import LegalRetriever
from app.config import HOST, PORT
import uvicorn
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("UkilBhaiBackend")

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Pre-load the dataset & build index on startup
    logger.info("Initializing Ukil Bhai Legal Retriever during startup...")
    LegalRetriever.get_instance()
    logger.info("Retriever initialized and ready to serve queries.")
    yield
    logger.info("Shutting down Ukil Bhai backend...")

app = FastAPI(
    title="Ukil Bhai AI Legal RAG Backend",
    description="Official Retrieval-Augmented Generation (RAG) backend for Ukil Bhai using the Bangladesh Legal Acts Dataset (BLAD).",
    version="1.0.0",
    lifespan=lifespan
)

# Enable CORS for cross-platform and web clients
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router)

if __name__ == "__main__":
    uvicorn.run("main:app", host=HOST, port=PORT, reload=False)
