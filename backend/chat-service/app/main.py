import httpx
from fastapi import FastAPI
from fastapi.routing import APIRoute
from fastapi.middleware.cors import CORSMiddleware
from prometheus_fastapi_instrumentator import Instrumentator
from app.chat import router as chat_router
from app.neo4j_driver import get_driver
from app.auth import PROFILE_ENDPOINT

from dotenv import load_dotenv

load_dotenv()

app = FastAPI(
    title="Chat Service",
    description="Exposes a hybrid GraphRAG endpoint using Neo4j",
    version="1.0.0",
)

Instrumentator().instrument(app).expose(app)

# CORS Configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(chat_router, prefix="/chat")

@app.get("/health/liveness")
async def liveness():
    return {"status": "alive"}

@app.get("/health/readiness")
async def readiness():
    try:
        async with httpx.AsyncClient(timeout=3.0) as client:
            resp = await client.get(PROFILE_ENDPOINT)
            if resp.status_code != 200:
                return {"status": "not ready", "reason": "user-management unavailable"}
    except Exception as e:
        return {"status": "not ready", "reason": f"user-management error: {str(e)}"}

    driver = get_driver()
    try:
        async with driver.session() as session:
            await session.run("RETURN 1")
    except Exception as e:
        return {"status": "not ready", "reason": f"neo4j error: {str(e)}"}

    return {"status": "ready"}
