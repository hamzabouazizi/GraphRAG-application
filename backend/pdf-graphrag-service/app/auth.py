from fastapi import FastAPI, UploadFile, File, HTTPException, Depends, Header
import requests
from app.config import settings

# fetch the current user from user-management service
async def get_current_user(authorization: str = Header(..., alias="Authorization")):
    """
    Verifies the user's JWT token via the /profile endpoint in the user-management backend.
    """
    try:
        response = requests.get(
            f"{settings.USER_MGMT_URL}/profile",
            headers={"Authorization": authorization},
            timeout=5,
        )
        if response.status_code != 200:
            raise HTTPException(status_code=401, detail="Unauthorized: invalid token")

        print("get_current_user called")
        return response.json()

    except requests.RequestException:
        raise HTTPException(status_code=500, detail="Could not validate user")