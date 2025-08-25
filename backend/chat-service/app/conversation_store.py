from app.neo4j_driver import get_driver
import uuid


async def ensure_conversation(conversation_id: str | None, user_email: str) -> str:
    """
    Make sure a Conversation node exists. Return its id.
    """
    cid = conversation_id or str(uuid.uuid4())
    driver = get_driver()
    async with driver.session() as session:
        await session.run(
            """
            MERGE (c:Conversation {id: $cid})
            ON CREATE SET c.user_email = $email,
                          c.created_at = timestamp(),
                          c.next_idx = 0
        """,
            {"cid": cid, "email": user_email},
        )
    return cid


async def load_history(
    conversation_id: str, user_email: str, limit: int = 10
) -> list[dict]:
    """
    Return the last `limit` turns as [{"role": "...", "content": "..."}], oldest→newest.
    """
    driver = get_driver()
    async with driver.session() as session:
        res = await session.run(
            """
            MATCH (c:Conversation {id: $cid, user_email: $email})-[:HAS_TURN]->(t:Turn)
            RETURN t.role AS role, t.content AS content, t.idx AS idx
            ORDER BY t.idx DESC
            LIMIT $limit
        """,
            {"cid": conversation_id, "email": user_email, "limit": limit},
        )
        rows = await res.data()

    # rows are newest→oldest; reverse to oldest→newest
    hist = [{"role": r["role"], "content": r["content"]} for r in reversed(rows)]
    return hist


async def append_turns(
    conversation_id: str, user_email: str, user_q: str, assistant_a: str
) -> None:
    """
    Append a user turn and an assistant turn, keeping an incrementing idx.
    """
    driver = get_driver()
    async with driver.session() as session:
        await session.run(
            """
            MATCH (c:Conversation {id: $cid, user_email: $email})
            WITH c, coalesce(c.next_idx, 0) AS i
            SET c.next_idx = i + 2
            CREATE (u:Turn {role: 'user',      content: $uq, idx: i,     ts: timestamp()})
            CREATE (a:Turn {role: 'assistant', content: $aa, idx: i + 1, ts: timestamp()})
            MERGE (c)-[:HAS_TURN]->(u)
            MERGE (c)-[:HAS_TURN]->(a)
        """,
            {
                "cid": conversation_id,
                "email": user_email,
                "uq": user_q,
                "aa": assistant_a,
            },
        )
