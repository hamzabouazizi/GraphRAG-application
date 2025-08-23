from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from app.auth import get_current_user
from app.neo4j_driver import get_driver
from app.embedding import (
    embed_text,
    cosine_similarity,
    rank_chunks,
    min_max_normalize,
    mmr_select,
)
from app.models import ChatRequest, ChatResponse
from fastapi.responses import JSONResponse
import openai
from openai import OpenAI
import os
import json
import numpy as np

router = APIRouter()
openai.api_key = os.getenv("OPENAI_API_KEY")
client = OpenAI()


def is_broad_question(q: str) -> bool:
    ql = (q or "").lower().strip()
    broad_words = [
        "overview",
        "about",
        "summary",
        "summarize",
        "explain",
        "what is this project",
    ]
    return len(ql.split()) <= 6 or any(w in ql for w in broad_words)


def dynamic_top_k(question: str, base_k: int) -> int:
    return 10 if is_broad_question(question) else base_k


class ChatRequest(BaseModel):
    question: str
    top_k: int = 5  # how many chunks to use
    alpha: float = 0.7  # blend: 1.0=cosine only, 0.0=BM25 only
    use_mmr: bool = True  # diversify results


class ChatResponse(BaseModel):
    answer: str


@router.post("/", response_model=ChatResponse)
async def chat_endpoint(request: ChatRequest, user: dict = Depends(get_current_user)):
    user_email = user.get("email")
    print("EMAIL from token:", user_email)
    print("DEBUG: question =", repr(request.question))

    if not user_email:
        raise HTTPException(status_code=400, detail="Missing user email")

    query_embedding = embed_text(request.question)

    try:
        print("DEBUG: query_embedding_dim =", len(query_embedding))
    except Exception as e:
        print("DEBUG: query_embedding type =", type(query_embedding), "err:", e)

    driver = get_driver()
    async with driver.session() as session:
        # fetch all chunks for this user (id, text, embedding, file_name, pdf_id)
        result = await session.run(
            """
            MATCH (u:User {email: $email})-[:UPLOADED]->(c:Chunk)
            RETURN
              c.id        AS id,
              c.text      AS text,
              c.embedding AS embedding,
              c.file_name AS file_name,
              c.pdf_id    AS pdf_id,
              c.page      AS page
            """,
            {"email": user_email},
        )
        chunks = await result.data()

        # BM25 for the same user
        bm25_res = await session.run(
            """
            CALL db.index.fulltext.queryNodes('chunkText', $q) YIELD node, score
            WHERE node.user_email = $email
            RETURN node.id AS id, score
            ORDER BY score DESC
            LIMIT 100
            """,
            {"q": request.question, "email": user_email},
        )
        bm25_hits = await bm25_res.data()

    print("DEBUG: total user chunks =", len(chunks))
    print("DEBUG: bm25 hits =", len(bm25_hits))

    ql = request.question.strip().lower()
    is_list_docs = (
        "what" in ql and "document" in ql and ("upload" in ql or "uploaded" in ql)
    ) or ("list" in ql and "document" in ql)
    if is_list_docs:
        files = sorted(
            {c.get("file_name") for c in (chunks or []) if c.get("file_name")}
        )
        answer = (
            "You uploaded: " + ", ".join(files)
            if files
            else "I don't see any uploaded documents."
        )
        return ChatResponse(answer=answer)

    if not chunks:
        raise HTTPException(status_code=404, detail="No chunks found for user")

    chunk_by_id = {c["id"]: c for c in chunks if c.get("id")}

    # cosine similarity for every chunk
    cosine_by_id = {}
    for c in chunks:
        emb = c.get("embedding")
        cid = c.get("id")
        if emb and cid:
            cosine_by_id[cid] = float(cosine_similarity(query_embedding, emb))

    # normalize BM25 scores
    bm25_by_id_raw = {
        row["id"]: float(row["score"]) for row in bm25_hits if row.get("id")
    }
    bm25_by_id = min_max_normalize(bm25_by_id_raw)

    # blend
    alpha = float(getattr(request, "alpha", 0.7))
    top_k = int(getattr(request, "top_k", 5))

    candidates = []
    for cid, c in chunk_by_id.items():
        cos = cosine_by_id.get(cid, 0.0)
        bm = bm25_by_id.get(cid, 0.0)
        final = alpha * cos + (1.0 - alpha) * bm
        candidates.append((final, c.get("embedding"), c))

    candidates.sort(key=lambda x: x[0], reverse=True)
    print(
        "DEBUG: sample fused scores (top 5):", [round(x[0], 4) for x in candidates[:5]]
    )

    # Dynamic top_k + MMR selection
    dyn_k = dynamic_top_k(request.question, top_k)

    use_mmr = bool(getattr(request, "use_mmr", True))
    if use_mmr:
        selected_chunks = mmr_select(candidates, k=dyn_k, lambda_=0.7)
    else:
        selected_chunks = [c[2] for c in candidates[:dyn_k]]

    print(
        f"DEBUG: dynamic_k used = {dyn_k}, selected_chunks = {len(selected_chunks)} (use_mmr={use_mmr})"
    )

    # Group all selected chunks under one file tag
    file_name = selected_chunks[0].get("file_name", "unknown")

    # Collect unique pages (sorted)
    pages = sorted({ch.get("page", "?") for ch in selected_chunks})

    # Build context with source tags for citations
    context = (
        f"[file:{file_name} pages:{','.join(map(str, pages))}]\n"
        + "\n\n---\n\n".join(ch.get("text", "") for ch in selected_chunks)
    )

    prompt = (
        "You are a RAG assistant. Answer ONLY with the context below.\n"
        "Cite sources like [file:... page:...].\n\n"
        f"Context:\n{context}\n\n"
        f"Question: {request.question}\n"
        "Answer:"
    )

    print("DEBUG: prompt first 400 chars =", repr(prompt[:400]))

    response = client.chat.completions.create(
        model="gpt-3.5-turbo",
        temperature=0,
        messages=[
            {
                "role": "system",
                "content": "Follow instructions strictly. Use only the provided context.",
            },
            {"role": "user", "content": prompt},
        ],
    )

    return ChatResponse(
        answer=response.model_dump()["choices"][0]["message"]["content"]
    )
