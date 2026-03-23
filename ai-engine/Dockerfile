# Estágio 1: Builder
FROM python:3.12-slim AS builder
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir --prefix=/install -r requirements.txt

# Estágio 2: Runtime
FROM python:3.12-slim-bookworm
WORKDIR /app
COPY --from=builder /install /usr/local
COPY . .

# Expondo a porta do FastAPI
EXPOSE 8000
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
