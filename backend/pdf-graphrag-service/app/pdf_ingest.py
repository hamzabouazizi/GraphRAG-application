import fitz
import json
from openai import OpenAI
from app.config import settings
import hashlib

client = OpenAI(api_key=settings.OPENAI_API_KEY)


def extract_and_chunk(
    pdf_bytes: bytes, max_tokens: int = 800
) -> tuple[list[str], list[int]]:
    """
    Extracts text per page and chunks each page separately so we keep page numbers.
    Returns:
      - chunks: list[str]  (each chunk's text)
      - pages:  list[int]  (same length; page number for each chunk, 1-based)
    """

    doc = fitz.open(stream=pdf_bytes, filetype="pdf")

    all_chunks: list[str] = []
    all_pages: list[int] = []

    # Loop page by page, chunk each page so we keep page numbers
    for page_index in range(len(doc)):
        page = doc[page_index]
        page_text = (page.get_text() or "").strip()
        if not page_text:
            continue

        # Ask LLM to chunk THIS PAGE ONLY
        prompt = (
            "You are a PDF knowledge assistant. "
            "Given the following SINGLE PAGE of text, split it into logical, self-contained chunks, "
            f"each no longer than {max_tokens} tokens. "
            "Return ONLY a JSON array of strings (no extra prose).\n\n"
            f"PAGE_NUMBER: {page_index + 1}\n"
            f"PAGE_TEXT:\n{page_text[:3000]}\n"
        )

        try:
            chat_response = client.chat.completions.create(
                model="gpt-3.5-turbo",
                messages=[{"role": "system", "content": prompt}],
                temperature=0.0,
            )
            content = (chat_response.choices[0].message.content or "").strip()

            # Try to parse JSON array of strings
            page_chunks = json.loads(content)
            if not isinstance(page_chunks, list) or not all(
                isinstance(c, str) for c in page_chunks
            ):
                raise ValueError("Invalid chunk format from LLM")

        except Exception:

            paragraphs = [p.strip() for p in page_text.split("\n\n") if p.strip()]
            page_chunks = []
            for para in paragraphs:

                for i in range(0, len(para), max_tokens * 4):
                    page_chunks.append(para[i : i + max_tokens * 4])

        # Collect with page numbers
        for ch in page_chunks:
            if ch and ch.strip():
                all_chunks.append(ch.strip())
                all_pages.append(page_index + 1)

    doc.close()

    return all_chunks, all_pages


def compute_pdf_hash(chunks: list[str]) -> str:
    full_text = "".join(chunks)
    return hashlib.sha256(full_text.encode("utf-8")).hexdigest()
