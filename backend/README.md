---
title: Ukil Bhai Legal RAG Backend
emoji: ⚖️
colorFrom: green
colorTo: emerald
sdk: docker
app_port: 7860
pinned: false
---

# Ukil Bhai - Legal RAG API Service

Official Retrieval-Augmented Generation (RAG) backend for **Ukil Bhai**, powered by the **Bangladesh Legal Acts Dataset (BLAD)** ([sakhadib/Bangladesh-Legal-Acts-Dataset](https://huggingface.co/datasets/sakhadib/Bangladesh-Legal-Acts-Dataset)).

## Features
- **Statute Retrieval**: Full-text BM25 search across 35,633+ legal sections from 1,281 active non-repealed acts.
- **Bilingual Legal Understanding**: Native token matching for both Bengali (`\u0980-\u09FF`) and English legal queries, including Bengali numeral normalization (`ধারা ৩০২` -> `302`).
- **AI Synthesis**: Powered by Groq Cloud (`qwen/qwen3.8-27b`) and Google Gemini with multi-tier fallback.
- **REST Endpoints**:
  - `GET /health` - Health check and index count
  - `POST /retrieve` - Search relevant sections
  - `POST /ask` - Get verified AI legal advice with citation badges

## Local Testing
```bash
pip install -r requirements.txt
python main.py
```
Then visit `http://localhost:7860/docs` for the interactive Swagger UI.
