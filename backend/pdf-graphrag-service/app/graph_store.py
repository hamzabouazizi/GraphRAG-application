import time
from neo4j.exceptions import ServiceUnavailable, ClientError
import uuid
from app.neo4j_driver import get_driver


_driver = get_driver()


def pdf_exists(pdf_hash: str, user_email: str) -> bool:
    """
    Return True if the user already uploaded a PDF with this hash.

    This checks the User -[:UPLOADED]-> PDF relationship rather than counting Chunk nodes.
    """
    with _driver.session() as session:
        result = session.run(
            """
            MATCH (u:User {email: $user_email})-[:UPLOADED]->(p:PDF {hash: $pdf_hash})
            RETURN count(p) AS cnt
            """,
            {"user_email": user_email, "pdf_hash": pdf_hash},
        )
        row = result.single()
        return bool(row and row.get("cnt", 0) > 0)


def write_chunks(
    chunks: list[str],
    embeddings: list[list[float]],
    pages: list[int],
    user_email: str,
    pdf_hash: str,
    file_name: str,
    file_size: int = None,
    num_pages: int = None,
    uploaded_at: str = None,
) -> None:
    """
    Store each text chunk + its embedding + its page in Neo4j.
    Creates PDF node if not exists and links to User.
    Creates Chunk nodes for PDF, skips if chunks already exist.
    """
    if not (len(chunks) == len(embeddings) == len(pages)):
        raise ValueError(
            f"Length mismatch: chunks={len(chunks)}, embeddings={len(embeddings)}, pages={len(pages)}"
        )
    with _driver.session() as session:
        # Ensure PDF node exists and get its ID
        pdf_result = session.run(
            """
            MERGE (p:PDF {hash: $pdf_hash})
            ON CREATE SET
                p.id = $new_pdf_id,
                p.file_name = $file_name,
                p.file_size = $file_size,
                p.num_pages = $num_pages,
                p.uploaded_at = datetime($uploaded_at)
                p.user_email = $user_email
            WITH p
            MATCH (u:User {email: $user_email})
            MERGE (u)-[:UPLOADED]->(p)
            RETURN p.id AS pdf_id
            """,
            {
                "user_email": user_email,
                "pdf_hash": pdf_hash,
                "new_pdf_id": str(uuid.uuid4()),
                "file_name": file_name,
                "file_size": file_size,
                "num_pages": num_pages,
                "uploaded_at": uploaded_at,
            },
        )
        pdf_id = pdf_result.single()["pdf_id"]
        # Prepare chunks with unique IDs
        rows = [
            {
                "chunk_id": f"{pdf_id}-{idx}",
                "text": text,
                "embedding": embedding,
                "page": int(page),
            }
            for idx, (text, embedding, page) in enumerate(zip(chunks, embeddings, pages))
        ]       

        # Only create chunks if they don't already exist
        tx = session.begin_transaction()
        try:
            tx.run(
                """
                UNWIND $rows AS row
                MATCH (p:PDF {id: $pdf_id})
                MERGE (c:Chunk {id: row.chunk_id})
                  ON CREATE SET
                    c.text = row.text,
                    c.embedding = row.embedding,
                    c.page = row.page,
                    c.pdf_id = p.id,
                    c.pdf_hash = $pdf_hash,
                    c.file_name = $file_name
                MERGE (p)-[:HAS_CHUNK]->(c)
                    """,
                {
                    "rows": rows,
                    "pdf_id": pdf_id,
                    "pdf_hash": pdf_hash,
                    "file_name": file_name,
                },
                )                                                      
            tx.commit()
        except Exception:
            tx.rollback()
            raise


def ensure_indexes(retries=10, delay=5):
    """
    Ensure required Neo4j constraints and indexes exist.
    Retries if Neo4j is not ready. Ignores 'already exists' errors.
    """
    statements = [
        # Users: unique email
        "CREATE CONSTRAINT user_email_unique IF NOT EXISTS FOR (u:User) REQUIRE u.email IS UNIQUE",
        # PDFs: unique hash
        "CREATE CONSTRAINT pdf_hash_unique IF NOT EXISTS FOR (p:PDF) REQUIRE p.hash IS UNIQUE",
        # Chunks: index on id
        "CREATE CONSTRAINT chunk_id_unique IF NOT EXISTS FOR (c:Chunk) REQUIRE c.id IS UNIQUE",
        # Full-text index on chunk text
        "CREATE FULLTEXT INDEX chunkText IF NOT EXISTS FOR (c:Chunk) ON EACH [c.text]",
    ]

    for attempt in range(1, retries + 1):
        try:
            with _driver.session() as session:
                for stmt in statements:
                    try:
                        session.run(stmt)
                        print(f"Executed: {stmt}")
                    except ClientError as e:
                        if "already exists" in str(e):
                            print(
                                f"Index/constraint already exists for statement: {stmt}"
                            )
                        else:
                            raise
            print("All Neo4j indexes/constraints ensured.")
            return
        except ServiceUnavailable:
            print(
                f"Neo4j not ready (attempt {attempt}/{retries}), retrying in {delay}s..."
            )
            time.sleep(delay)

    raise RuntimeError("Failed to connect to Neo4j after multiple retries")


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
