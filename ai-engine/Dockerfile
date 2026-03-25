# Estágio 1: Builder (Onde instalamos as dependências)
FROM python:3.12-alpine AS builder

WORKDIR /app

# Instalando o uv para gerenciar dependências com velocidade
COPY --from=ghcr.io/astral-sh/uv:latest /uv /bin/uv

# Copiamos os arquivos de projeto primeiro (para cache do Docker)
COPY pyproject.toml .

# Instalamos as dependências em uma pasta específica
RUN uv pip install --system --no-cache -r pyproject.toml

# Estágio 2: Runtime (Imagem final, leve e segura)
FROM python:3.12-alpine

WORKDIR /app

# Copiamos apenas as bibliotecas instaladas no estágio anterior
COPY --from=builder /usr/local/lib/python3.12/site-packages /usr/local/lib/python3.12/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin

# Copiamos o código da aplicação
COPY main.py .

EXPOSE 8000

# Comando para rodar a API
CMD ["python", "main.py"]
