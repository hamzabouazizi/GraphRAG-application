from openai import OpenAI
from app.config import settings

# Initialize the OpenAI client
_client = OpenAI(api_key=settings.OPENAI_API_KEY)


def compute_embeddings(
    chunks: list[str], model: str = "text-embedding-ada-002"
) -> list[list[float]]:
    """
    Given a list of text chunks, call OpenAI's embedding API
    to convert each chunk into a vector.

    :param chunks: List of text strings to embed
    :param model: Embedding model to use (default: text-embedding-ada-002)
    :return: List of embedding vectors (each a list of floats)
    """
    embeddings: list[list[float]] = []

    for chunk in chunks:
        # Create an embedding for this chunk
        resp = _client.embeddings.create(model=model, input=chunk)
        # Extract the embedding vector
        vector = resp.data[0].embedding
        embeddings.append(vector)

    return embeddings
