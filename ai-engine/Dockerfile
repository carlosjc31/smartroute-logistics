# Estágio 1: dependências (uv com versão fixa)
FROM python:3.12-slim AS builder

WORKDIR /app
COPY --from=ghcr.io/astral-sh/uv:0.5.11 /uv /bin/uv

COPY pyproject.toml .
RUN uv pip compile pyproject.toml -o requirements.lock \
    && uv pip install --system --no-cache -r requirements.lock

# Estágio 2: runtime enxuto, sem uv nem cache de build, rodando como usuário sem privilégios
FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid app --no-create-home app

WORKDIR /app
COPY --from=builder /usr/local/lib/python3.12/site-packages /usr/local/lib/python3.12/site-packages
COPY --from=builder /usr/local/bin/uvicorn /usr/local/bin/uvicorn
# Só o serviço e o modelo versionado com o hash; dados e scripts de treino ficam fora da imagem
COPY --chown=root:root app/ app/
COPY --chown=root:root models/ models/

USER app
EXPOSE 8000

HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=5 \
  CMD python -c "import urllib.request,sys; sys.exit(0 if b'Active' in urllib.request.urlopen('http://localhost:8000/health', timeout=2).read() else 1)"

CMD ["python", "-m", "app"]
