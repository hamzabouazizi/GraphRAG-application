# auth.py
import os
import httpx
from fastapi import HTTPException, Header
from starlette.status import HTTP_401_UNAUTHORIZED

from dotenv import load_dotenv

load_dotenv()

USER_MANAGEMENT_URL = os.getenv("USER_MGMT_URL")
if not USER_MANAGEMENT_URL:
    raise RuntimeError("USER_MGMT_URL is not set in the environment")

PROFILE_ENDPOINT = f"{USER_MANAGEMENT_URL}/profile"


async def get_current_user(authorization: str = Header(...)):
    if not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=HTTP_401_UNAUTHORIZED,
            detail="Invalid Authorization header format",
        )

    token = authorization[7:]

    try:
        async with httpx.AsyncClient(timeout=5) as client:
            response = await client.get(
                PROFILE_ENDPOINT, headers={"Authorization": f"Bearer {token}"}
            )
    except httpx.RequestError:
        raise HTTPException(
            status_code=HTTP_401_UNAUTHORIZED, detail="User service unreachable"
        )

    if response.status_code != 200:
        raise HTTPException(
            status_code=HTTP_401_UNAUTHORIZED, detail="Invalid or expired token"
        )

    try:
        return response.json()
    except ValueError:
        raise HTTPException(
            status_code=HTTP_401_UNAUTHORIZED,
            detail="Invalid response from user service",
        )
