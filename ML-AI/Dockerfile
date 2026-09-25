FROM python:3.12-slim
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends libgomp1 \
    && rm -rf /var/lib/apt/lists/*
COPY requirements.txt .
RUN pip install  \
    --no-cache-dir \
    --only-binary=:all: -r \
    requirements.txt
RUN useradd --create-home --shell /usr/sbin/nologin appuser
COPY predict_service.py .
COPY ai_usage_model.joblib .
USER appuser
EXPOSE 8001
CMD ["python", "-m", "uvicorn", "predict_service:app", "--host", "0.0.0.0", "--port", "8001"]