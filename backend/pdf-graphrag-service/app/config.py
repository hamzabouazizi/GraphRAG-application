import os
from dotenv import load_dotenv
load_dotenv()

class Settings:
    OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
    USER_MGMT_URL    = os.getenv("USER_MGMT_URL")

settings = Settings()
