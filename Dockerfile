FROM mcr.microsoft.com/playwright/python:v1.62.0-noble

WORKDIR /app

COPY scripts/requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

COPY scripts/ ./scripts/

CMD ["uvicorn", "scripts.main:app", "--host", "0.0.0.0", "--port", "10000"]