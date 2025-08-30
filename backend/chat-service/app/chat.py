from fastapi import APIRouter, Depends, HTTPException, Query, Request
from sse_starlette.sse import EventSourceResponse
from typing import Optional
from pydantic import BaseModel
from app.auth import get_current_user, get_current_user_for_sse
from app.neo4j_driver import get_driver
from app.embedding import (
    embed_text,
    cosine_similarity,
    rank_chunks,
    min_max_normalize,
    mmr_select,
)
from app.conversation_store import ensure_conversation, load_history, append_turns
from fastapi.responses import JSONResponse
import openai
import uuid
from openai import OpenAI
import os
import json
import numpy as np

router = APIRouter()
openai.api_key = os.getenv("OPENAI_API_KEY")
client = OpenAI()


def condense_question(history: list[dict], follow_up: str) -> str:
    """
    Use last ~10 turns to rewrite a follow-up into a standalone question.
    If no history, just return the original question.
    """
    if not history:
        return (follow_up or "").strip()

    prompt = (
        "Rewrite the follow-up question into a standalone question that includes any "
        "necessary context from the conversation history.\n\n"
        f"History:\n{json.dumps(history[-10:], indent=2)}\n\n"
        f"Follow-up: {follow_up}\n\n"
        "Standalone:"
    )
    resp = client.chat.completions.create(
        model="gpt-3.5-turbo",
        temperature=0,
        messages=[{"role": "system", "content": prompt}],
    )
    return (resp.choices[0].message.content or "").strip()


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
    conversation_id: str | None = None
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

    conv_id = await ensure_conversation(request.conversation_id, user_email)

    # load the last turns (up to 10)
    history = await load_history(conv_id, user_email, limit=10)

    # Condense follow-up standalone
    standalone_q = condense_question(history, request.question)
    print("DEBUG: standalone question =", repr(standalone_q))

    query_embedding = embed_text(standalone_q)

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
            {"q": standalone_q, "email": user_email},
        )
        bm25_hits = await bm25_res.data()

    print("DEBUG: total user chunks =", len(chunks))
    print("DEBUG: bm25 hits =", len(bm25_hits))

    ql = request.question.strip().lower()
    is_list_docs = (
        "what" in ql
        and ("document" in ql or "pdf" in ql)
        and ("upload" in ql or "uploaded" in ql)
    ) or ("list" in ql and ("document" in ql or "pdf" in ql))
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

    await append_turns(
        conv_id,
        user_email,
        request.question,
        response.model_dump()["choices"][0]["message"]["content"],
    )

    return ChatResponse(
        answer=response.model_dump()["choices"][0]["message"]["content"]
    )


@router.get("/stream")
async def chat_stream_endpoint(
    request: Request,
    question: str = Query(..., description="User question"),
    conversation_id: Optional[str] = Query(
        None, description="Optional conversation id"
    ),
    top_k: int = Query(5, description="How many chunks to consider"),
    alpha: float = Query(
        0.7, description="Blend weight: 1.0=cosine only, 0.0=BM25 only"
    ),
    use_mmr: bool = Query(True, description="Apply MMR to diversify results"),
    token: Optional[str] = Query(None, description="JWT token fallback for SSE"),
):
    """
    SSE streaming endpoint that:
      - authenticates the user (Authorization header OR ?token=)
      - optionally condenses follow-up questions (if conversation_id provided)
      - runs retrieval (embedding + BM25 + fusion + MMR)
      - builds the RAG prompt (single file header + pages list)
      - streams the LLM response tokens as SSE 'token' events
    """
    user = await get_current_user_for_sse(request, token)
    user_email = user.get("email")
    if not user_email:
        raise HTTPException(status_code=400, detail="Missing user email")
    standalone_q = question
    conv_id = conversation_id
    if conversation_id is not None:
        conv_id = await ensure_conversation(conversation_id, user_email)
        history = await load_history(conv_id, user_email, limit=10)
        try:
            standalone_q = condense_question(history, question)
        except Exception:
            standalone_q = question
    query_embedding = embed_text(standalone_q)
    driver = get_driver()
    async with driver.session() as session:
        # fetch all chunks
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
        bm25_res = await session.run(
            """
            CALL db.index.fulltext.queryNodes('chunkText', $q) YIELD node, score
            WHERE node.user_email = $email
            RETURN node.id AS id, score
            ORDER BY score DESC
            LIMIT 100
            """,
            {"q": standalone_q, "email": user_email},
        )
        bm25_hits = await bm25_res.data()

    print("DEBUG: total user chunks =", len(chunks))
    print("DEBUG: bm25 hits =", len(bm25_hits))

    if not chunks:

        async def empty_gen():
            yield {"event": "error", "data": "No chunks found for user"}
            yield {"event": "end", "data": "DONE"}

        return EventSourceResponse(empty_gen())
    chunk_by_id = {c["id"]: c for c in chunks if c.get("id")}
    cosine_by_id = {}
    for c in chunks:
        emb = c.get("embedding")
        cid = c.get("id")
        if emb and cid:
            cosine_by_id[cid] = float(cosine_similarity(query_embedding, emb))

    bm25_by_id_raw = {
        row["id"]: float(row["score"]) for row in bm25_hits if row.get("id")
    }
    bm25_by_id = min_max_normalize(bm25_by_id_raw)
    candidates = []
    for cid, c in chunk_by_id.items():
        cos = cosine_by_id.get(cid, 0.0)
        bm = bm25_by_id.get(cid, 0.0)
        final = float(alpha) * cos + (1.0 - float(alpha)) * bm
        candidates.append((final, c.get("embedding"), c))

    candidates.sort(key=lambda x: x[0], reverse=True)
    print(
        "DEBUG: sample fused scores (top 5):", [round(x[0], 4) for x in candidates[:5]]
    )
    dyn_k = dynamic_top_k(standalone_q, top_k)
    if use_mmr:
        selected_chunks = mmr_select(candidates, k=dyn_k, lambda_=0.7)
    else:
        selected_chunks = [c[2] for c in candidates[:dyn_k]]

    print(
        f"DEBUG: dynamic_k used = {dyn_k}, selected_chunks = {len(selected_chunks)} (use_mmr={use_mmr})"
    )

    if not selected_chunks:

        async def empty_gen2():
            yield {"event": "error", "data": "No candidate chunks selected"}
            yield {"event": "end", "data": "DONE"}

        return EventSourceResponse(empty_gen2())
    file_name = selected_chunks[0].get("file_name", "unknown")
    page_values = sorted(
        {p for p in (ch.get("page") for ch in selected_chunks) if p is not None}
    )
    pages_str = ",".join(str(p) for p in page_values) if page_values else "?"
    context = f"[file:{file_name} pages:{pages_str}]\n" + "\n\n---\n\n".join(
        ch.get("text", "") for ch in selected_chunks
    )

    prompt = (
        "You are a RAG assistant. Answer ONLY with the context below.\n"
        "Cite sources like [file:... page:...].\n\n"
        f"Context:\n{context}\n\n"
        f"Question: {question}\n"
        "Answer:"
    )

    print("DEBUG: prompt first 400 chars =", repr(prompt[:400]))

    async def event_generator():
        yield {"event": "start", "data": "ok"}

        full_parts: list[str] = []

        try:
            with client.chat.completions.stream(
                model="gpt-3.5-turbo",
                temperature=0,
                messages=[
                    {
                        "role": "system",
                        "content": "Follow instructions strictly. Use only the provided context.",
                    },
                    {"role": "user", "content": prompt},
                ],
            ) as stream:
                for event in stream:
                    if getattr(event, "type", None) in (
                        "content.delta",
                        "message.delta",
                    ) and getattr(event, "delta", None):
                        token_text = event.delta
                        full_parts.append(token_text)
                        yield {"event": "token", "data": token_text}
                    elif getattr(event, "type", None) in (
                        "message.stop",
                        "response.completed",
                        "message.complete",
                    ):
                        break

                try:
                    final = stream.get_final_response()
                    final_text = ""
                    if final is not None:
                        try:
                            final_text = final.model_dump().get("choices", [])[0].get("message", {}).get("content", "")  # type: ignore
                        except Exception:
                            final_text = "".join(full_parts)
                    else:
                        final_text = "".join(full_parts)
                except Exception:
                    final_text = "".join(full_parts)

        except Exception as e:
            yield {"event": "error", "data": f"LLM stream error: {str(e)}"}
            yield {"event": "end", "data": "DONE"}
            return

        yield {"event": "end", "data": "".join(full_parts)}

        if conv_id is not None:
            try:
                await append_turns(conv_id, user_email, question, "".join(full_parts))
            except Exception as e:
                print("WARN: failed to append_turns:", e)

    return EventSourceResponse(event_generator(), media_type="text/event-stream")
