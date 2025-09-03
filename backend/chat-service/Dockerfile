# Builder Stage
FROM python:3.12-slim AS builder

# Set working directory
WORKDIR /app

# Install system dependencies 
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Install dependencies
COPY requirements.txt .
RUN pip install --upgrade pip setuptools wheel && pip install --prefix=/install -r requirements.txt

# base runtime
FROM python:3.12-slim AS base

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PATH="/home/appuser/.local/bin:$PATH"

WORKDIR /app

COPY --from=builder /install /usr/local

COPY app ./app

RUN useradd -m appuser \
    && chown -R appuser:appuser /app /usr/local

USER appuser

# Dev Stage
FROM base AS dev
RUN pip install --upgrade pip \
    && pip install uvicorn[standard] watchdog python-dotenv
EXPOSE 8001
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001", "--reload"]

# Prod Stage 
FROM base AS prod
RUN pip install --upgrade pip \
    && pip install gunicorn uvicorn[standard]
EXPOSE 8001
CMD ["gunicorn", "-k", "uvicorn.workers.UvicornWorker", "-w", "4", "-b", "0.0.0.0:8001", "app.main:app"]
