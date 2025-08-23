# neo4j_driver.py
import os
from dotenv import load_dotenv
from neo4j import AsyncGraphDatabase
from fastapi import HTTPException

load_dotenv()

NEO4J_URI = os.getenv("NEO4J_URI")
NEO4J_USER = os.getenv("NEO4J_USER")
NEO4J_PASSWORD = os.getenv("NEO4J_PASSWORD")

if not all([NEO4J_URI, NEO4J_USER, NEO4J_PASSWORD]):
    raise RuntimeError("Neo4j connection environment variables are not fully set")

driver = AsyncGraphDatabase.driver(NEO4J_URI, auth=(NEO4J_USER, NEO4J_PASSWORD))


def get_driver():
    return driver


async def close_driver():
    await driver.close()
