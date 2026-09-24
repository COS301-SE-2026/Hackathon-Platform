FROM python:3.12-slim
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends libgomp1 \
    && rm -rf /var/lib/apt/list/*
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY predict_service.py .
COPY ai_usage_model.joblib .
EXPOSE 8001
CMD ["python", "-m", "uvicorn", "predict_service:app", "--host", "0.0.0.0", "--port", "8001"]