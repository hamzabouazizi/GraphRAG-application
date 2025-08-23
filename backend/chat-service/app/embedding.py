from openai import OpenAI
import os
import numpy as np
from dotenv import load_dotenv

load_dotenv()

client = OpenAI()


def embed_text(text: str) -> list[float]:
    response = client.embeddings.create(input=text, model="text-embedding-ada-002")
    return response.data[0].embedding


def cosine_similarity(a, b):
    a = np.array(a)
    b = np.array(b)
    return np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))


def rank_chunks(chunks, query_embedding, top_k=5):
    scored = []
    for chunk in chunks:
        embedding = chunk.get("embedding")
        if not embedding:
            continue
        score = cosine_similarity(query_embedding, embedding)
        scored.append((score, chunk))
    scored.sort(reverse=True, key=lambda x: x[0])
    return [chunk for _, chunk in scored[:top_k]]


def min_max_normalize(scores_by_id: dict[str, float]) -> dict[str, float]:
    if not scores_by_id:
        return {}
    vals = list(scores_by_id.values())
    mn, mx = min(vals), max(vals)
    if mx == mn:

        return {k: 1.0 for k in scores_by_id}
    return {k: (v - mn) / (mx - mn) for k, v in scores_by_id.items()}


def mmr_select(candidates, k=5, lambda_=0.7):
    """
    candidates: list of tuples (final_score, embedding_vector(list[float]), chunk_dict)
    returns: list[chunk_dict]
    """
    if not candidates:
        return []

    rest = sorted(candidates, key=lambda x: x[0], reverse=True)
    selected = []
    while rest and len(selected) < k:
        if not selected:
            selected.append(rest.pop(0))
            continue
        best_idx, best_val = None, None
        for i, cand in enumerate(rest):
            rel = cand[0]

            max_sim = max(cosine_similarity(cand[1], s[1]) for s in selected)
            mmr_score = lambda_ * rel - (1 - lambda_) * max_sim
            if best_val is None or mmr_score > best_val:
                best_val, best_idx = mmr_score, i
        selected.append(rest.pop(best_idx))
    return [c[2] for c in selected]
