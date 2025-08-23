import os
from dotenv import load_dotenv
load_dotenv()

class Settings:
    OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
    NEO4J_URI        = os.getenv("NEO4J_URI")
    NEO4J_USER       = os.getenv("NEO4J_USER")
    NEO4J_PASSWORD   = os.getenv("NEO4J_PASSWORD")
    USER_MGMT_URL    = os.getenv("USER_MGMT_URL")

settings = Settings()
