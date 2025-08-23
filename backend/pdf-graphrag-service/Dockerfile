# Builder stage
FROM python:3.12-slim AS builder
WORKDIR /app

# Minimal build tools 
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --upgrade pip setuptools wheel \
    && pip install --prefix=/install -r requirements.txt

# Base runtime stage
FROM python:3.12-slim AS base
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PATH="/home/appuser/.local/bin:$PATH"

WORKDIR /app

# copy installed packages from builder
COPY --from=builder /install /usr/local

# copy app sources
COPY app ./app

# create non-root user and fix ownership of app and local bin
RUN useradd -m appuser \
    && chown -R appuser:appuser /app /usr/local

USER appuser

# Dev stage (hot-reload)
FROM base AS dev
RUN pip install --upgrade pip \
    && pip install uvicorn[standard] watchdog python-dotenv

EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--reload"]

# Prod stage
FROM base AS prod
RUN pip install --upgrade pip \
    && pip install gunicorn uvicorn[standard]

EXPOSE 8000
CMD ["gunicorn", "-k", "uvicorn.workers.UvicornWorker", "-w", "4", "-b", "0.0.0.0:8000", "app.main:app"]
