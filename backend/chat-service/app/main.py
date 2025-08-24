from fastapi import FastAPI
from fastapi.routing import APIRoute
from fastapi.middleware.cors import CORSMiddleware
from app.chat import router as chat_router

from dotenv import load_dotenv

load_dotenv()

app = FastAPI(
    title="Chat Service",
    description="Exposes a hybrid GraphRAG endpoint using Neo4j",
    version="1.0.0",
)

# CORS Configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(chat_router, prefix="/chat")


# basic health check endpoint
@app.get("/")
async def root():
    return {"status": "Chat service is running."}
