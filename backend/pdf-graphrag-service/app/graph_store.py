from neo4j import GraphDatabase
from app.config import settings
import uuid

_driver = GraphDatabase.driver(
    settings.NEO4J_URI,
    auth=(settings.NEO4J_USER, settings.NEO4J_PASSWORD),
    max_connection_lifetime=1000,
)


def pdf_exists(pdf_hash: str, user_email: str) -> bool:
    """
    Check if a PDF with this hash has already been uploaded by this user.
    """
    with _driver.session() as session:
        result = session.run(
            """
            MATCH (c:Chunk {user_email: $user_email, pdf_hash: $pdf_hash})
            RETURN count(c) AS count
            """,
            {"user_email": user_email, "pdf_hash": pdf_hash}
        )
        return result.single()["count"] > 0


def write_chunks(
    chunks: list[str],
    embeddings: list[list[float]],
    pages: list[int],
    user_email: str,
    pdf_hash: str,
    file_name: str
) -> None:
    """
    Store each text chunk + its embedding + its page in Neo4j.
    Skip if same PDF hash has already been uploaded by the user.
    """
    if not (len(chunks) == len(embeddings) == len(pages)):
        raise ValueError(
            f"Length mismatch: chunks={len(chunks)}, embeddings={len(embeddings)}, pages={len(pages)}"
        )
    with _driver.session() as session:
        # Check if already exists 
        result = session.run(
            """
            MATCH (c:Chunk {user_email: $user_email, pdf_hash: $pdf_hash})
            RETURN count(c) AS count
            """,
            {"user_email": user_email, "pdf_hash": pdf_hash}
        )

        if result.single()["count"] > 0:
            print(" Duplicate PDF detected — skipping chunk upload.")
            return

        # Insert chunks
        pdf_id = str(uuid.uuid4())

        for idx, (text, embedding, page) in enumerate(zip(chunks, embeddings, pages)):
            chunk_id = f"{user_email}-{pdf_id}-{idx}"
            page = int(pages[idx])
            session.run(
                """
                MERGE (u:User {email: $user_email})
                CREATE (c:Chunk {
                    id: $chunk_id,
                    text: $text,
                    embedding: $embedding,
                    user_email: $user_email,
                    pdf_id: $pdf_id,
                    pdf_hash: $pdf_hash,
                    file_name: $file_name,
                    page: $page
                })
                MERGE (u)-[:UPLOADED]->(c)
                """,
                {
                    "user_email": user_email,
                    "chunk_id": chunk_id,
                    "text": text,
                    "embedding": embedding,
                    "pdf_id": pdf_id,
                    "pdf_hash": pdf_hash,
                    "file_name": file_name,
                    "page": int(page),
                }
            )

def ensure_indexes():
    with _driver.session() as session:
        session.run("""
        CREATE FULLTEXT INDEX chunkText IF NOT EXISTS FOR (c:Chunk) ON EACH [c.text]
        """)

def check_connection() -> bool:
    """
    Verify Neo4j connection is alive.
    """
    try:
        with _driver.session() as session:
            session.run("RETURN 1")
        return True
    except Exception as e:
        print(f"Neo4j connection failed: {e}")
        return False


