from fastapi import FastAPI, UploadFile, File, HTTPException, Depends, Header
from fastapi.responses import JSONResponse
import requests
import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import hashlib
from app.graph_store import ensure_indexes

from app.pdf_ingest import extract_and_chunk
from app.graph_store import write_chunks
from app.config import settings
from app.embedding import compute_embeddings
from app.graph_store import write_chunks, pdf_exists


app = FastAPI(title="PDF GraphRAG Service")

# Ensure index exists on startup
ensure_indexes()

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


# Dependency to fetch the current user from user-management service
async def get_current_user(authorization: str = Header(..., alias="Authorization")):
    """
    Verifies the user's JWT token via the /profile endpoint in the user-management backend.
    """
    try:
        response = requests.get(
            f"{settings.USER_MGMT_URL}/profile",
            headers={"Authorization": authorization},
            timeout=5,
        )
        if response.status_code != 200:
            raise HTTPException(status_code=401, detail="Unauthorized: invalid token")

        print("get_current_user called")
        return response.json()

    except requests.RequestException:
        raise HTTPException(status_code=500, detail="Could not validate user")


@app.post("/upload-pdf")
async def upload_pdf(
    file: UploadFile = File(...), user: dict = Depends(get_current_user)
):
    if not file.filename.lower().endswith(".pdf"):
        raise HTTPException(status_code=400, detail="Only PDF files are supported.")

    # Read and hash the PDF bytes
    pdf_bytes = await file.read()
    pdf_hash = hashlib.sha256(pdf_bytes).hexdigest()
    print("PDF Hash:", pdf_hash)

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
        )
        return JSONResponse(
            content={
                "message": "PDF processed and stored in Neo4j",
                "chunks": len(chunks),
                "user_id": user_email,
                "pdf_hash": pdf_hash,
                "file_name": file.filename,
            }
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
