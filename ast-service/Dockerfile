FROM python:3.11-slim

RUN groupadd --gid 1000 appuser \
    && useradd --uid 1000 --gid appuser --create-home --shell /usr/sbin/nologin appuser

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .

RUN pip install --no-cache-dir --only-binary=:all: \
        torch==2.4.1 --index-url https://download.pytorch.org/whl/cpu \
    && pip install --no-cache-dir --only-binary=:all: -r requirements.txt \
    && python -c "from transformers import AutoTokenizer, AutoModel; \
        AutoTokenizer.from_pretrained('microsoft/codebert-base'); \
        AutoModel.from_pretrained('microsoft/codebert-base')"

COPY normalize.py app.py embeddings.py ./

RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8008

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
    CMD curl -f http://localhost:8008/health || exit 1

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8008", "--workers", "1"]