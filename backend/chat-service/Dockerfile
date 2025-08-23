# Base Stage
FROM python:3.12-slim AS base

# Set working directory
WORKDIR /app

# Prevent Python from writing pyc files & buffer stdout/stderr
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

# Install system dependencies 
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Install dependencies
COPY requirements.txt .
RUN pip install --upgrade pip && pip install --no-cache-dir -r requirements.txt

# Copy the application code
COPY . .

# Dev Stage
FROM base AS dev
RUN pip install --no-cache-dir uvicorn[standard] watchdog
EXPOSE 8001
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001", "--reload"]

# Prod Stage 
FROM base AS prod
RUN pip install --no-cache-dir gunicorn uvicorn[standard]
EXPOSE 8001
CMD ["gunicorn", "-k", "uvicorn.workers.UvicornWorker", "-w", "4", "-b", "0.0.0.0:8001", "app.main:app"]
