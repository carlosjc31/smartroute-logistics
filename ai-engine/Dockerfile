# Estágio 1: Builder (Usando SLIM para suportar Scikit-Learn e Numpy)
FROM python:3.12-slim AS builder

WORKDIR /app

# Instalando o uv para gerenciar dependências com velocidade
COPY --from=ghcr.io/astral-sh/uv:latest /uv /bin/uv

# Copiamos os arquivos de configuração
COPY pyproject.toml .

# Compilamos o TOML em requirements e instalamos rapidamente
RUN uv pip compile pyproject.toml -o requirements.txt
RUN uv pip install --system --no-cache -r requirements.txt

# Estágio 2: Runtime (Imagem final, usando SLIM)
FROM python:3.12-slim

WORKDIR /app

# Copiamos as bibliotecas de ML instaladas no estágio 1
COPY --from=builder /usr/local/lib/python3.12/site-packages /usr/local/lib/python3.12/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin

# Copiamos TODO o código do projeto e o modelo treinado (.pkl)
COPY . .

EXPOSE 8000

# Comando para rodar a API
CMD ["python", "main.py"]
