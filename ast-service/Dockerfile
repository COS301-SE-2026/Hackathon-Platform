FROM python:3.11-slim

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .

RUN pip install --no-cache-dir torch==2.4.1 --index-url https://download.pytorch.org/whl/cpu \
    && pip install --no-cache-dir -r requirements.txt

RUN python -c "from transformers import AutoTokenizer, AutoModel; \
    AutoTokenizer.from_pretrained('microsoft/codebert-base'); \
    AutoModel.from_pretrained('microsoft/codebert-base')"

COPY normalize.py app.py embeddings.py ./

EXPOSE 8008

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
    CMD curl -f http://localhost:8008/health || exit 1

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8008", "--workers", "1"]