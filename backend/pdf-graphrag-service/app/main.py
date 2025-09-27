from datetime import datetime, timezone
import os
import httpx
from fastapi import FastAPI, UploadFile, File, HTTPException, Depends, Header
from fastapi.responses import JSONResponse
from PyPDF2 import PdfReader
import requests
import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import hashlib
from prometheus_fastapi_instrumentator import Instrumentator
from app.graph_store import ensure_indexes

from app.pdf_ingest import compute_pdf_hash, extract_and_chunk
from app.graph_store import write_chunks
from app.graph_store import check_connection
from app.config import settings
from app.embedding import compute_embeddings
from app.graph_store import write_chunks, pdf_exists
from app.pdf_store import save_pdf
from app.neo4j_driver import close_driver
from app.auth import get_current_user


app = FastAPI(title="PDF GraphRAG Service")

Instrumentator().instrument(app).expose(app)


@app.on_event("startup")
def startup_event():
    ensure_indexes(retries=10, delay=5)

# Allow requests from React
origins = ["http://localhost", "http://localhost:3000"]


# CORS middleware to allow React to talk to FastAPI
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.post("/upload-pdf")
async def upload_pdf(
    file: UploadFile = File(...), user: dict = Depends(get_current_user)
):
    if not file.filename.lower().endswith(".pdf"):
        raise HTTPException(status_code=400, detail="Only PDF files are supported.")
    
    pdf_hash, pdf_bytes = await compute_pdf_hash(file)
    
    saved_path = await save_pdf(file, user["email"])

    uploaded_at = datetime.now(timezone.utc).isoformat()
    file_size = len(pdf_bytes)
    print("PDF Hash:", pdf_hash)
        
    try:
        pdf_reader = PdfReader(saved_path)
        num_pages = len(pdf_reader.pages)
    except Exception:
        num_pages = None
    

    user_email = user["email"]
    print("User info from /profile:", user)

    # Check if the PDF already exists for this user
    if pdf_exists(pdf_hash, user_email):
        return JSONResponse(
            content={
                "message": "This PDF has already been uploaded by this user.",
                "pdf_hash": pdf_hash,
                "user_id": user_email,
                "chunks": 0,
                "file_name": file.filename,
            }
        )

    try:
        chunks, pages = extract_and_chunk(pdf_bytes)
        embeddings = compute_embeddings(chunks)
        write_chunks(
            chunks,
            embeddings,
            pages=pages,
            user_email=user_email,
            pdf_hash=pdf_hash,
            file_name=file.filename,
            file_size=file_size,
            num_pages=num_pages,
            uploaded_at=uploaded_at,
        )
        return JSONResponse(
            content={
                "message": "PDF processed and stored in Neo4j",
                "chunks": len(chunks),
                "user_id": user_email,
                "pdf_hash": pdf_hash,
                "file_name": file.filename,
                "file_size": file_size,
                "num_pages": num_pages,
                "uploaded_at": uploaded_at,
            }
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    
@app.get("/health/liveness")
async def liveness():
    return {"status": "alive"}

@app.get("/health/readiness")
async def readiness():
    try:
        async with httpx.AsyncClient(timeout=3.0) as client:
            resp = await client.get(f"{settings.USER_MGMT_URL}/profile")
            if resp.status_code != 200:
                return {"status": "not ready", "reason": "user-management unavailable"}
    except Exception as e:
        return {"status": "not ready", "reason": f"user-management error: {str(e)}"}

    if not check_connection():
        return {"status": "not ready", "reason": "neo4j unavailable"}

    return {"status": "ready"}


if __name__ == "__main__":
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)

@app.on_event("shutdown")
def shutdown_event():
    close_driver()
