"""`python -m app`: sobe o serviço na porta 8000."""
import uvicorn

if __name__ == "__main__":
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000)
