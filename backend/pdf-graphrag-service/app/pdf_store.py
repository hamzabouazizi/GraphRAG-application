from pathlib import Path
from fastapi import UploadFile, HTTPException 


PDF_STORAGE = Path("/app/storage/pdfs")
PDF_STORAGE.mkdir(parents=True, exist_ok=True)

async def save_pdf(file: UploadFile, user_email: str) -> str:
    """
    Save an uploaded PDF under a user-specific folder.
    Returns the path where the file was saved.
    """
    user_dir = PDF_STORAGE / user_email
    user_dir.mkdir(parents=True, exist_ok=True)

    file_path = user_dir / file.filename
    with open(file_path, "wb") as f:
        f.write(await file.read())

    return str(file_path)

async def list_user_pdfs(user_email: str) -> list[str]:
    """
    List all PDF filenames uploaded by the given user.
    """
    user_dir = PDF_STORAGE / user_email
    if not await user_dir.exists():
        return []

    pdf_files = [f.name for f in await user_dir.iterdir() if f.is_file() and f.suffix.lower() == ".pdf"]
    return pdf_files


async def delete_user_pdf(user_email: str, filename: str) -> None:
    """
    Delete a specific PDF uploaded by the user.
    Raises HTTPException if file does not exist.
    """
    file_path = PDF_STORAGE / user_email / filename
    if not await file_path.exists():
        raise HTTPException(status_code=404, detail=f"PDF '{filename}' not found for user '{user_email}'")

    await file_path.unlink()