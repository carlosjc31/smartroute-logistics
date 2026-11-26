import hashlib
import hmac
import logging
import os
from pathlib import Path

from fastapi import Depends, FastAPI, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel, Field
import joblib
import numpy as np
import uvicorn

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
log = logging.getLogger("ai-engine")

# Token de serviço exigido do backend-core. Sem ele o serviço não sobe (fail closed).
SERVICE_TOKEN = os.getenv("SERVICE_TOKEN", "")
if not SERVICE_TOKEN:
    raise RuntimeError("SERVICE_TOKEN não definido: o ai-engine não aceita chamadas sem autenticação")

# Caminho relativo a este arquivo, para não depender do diretório de execução.
MODEL_PATH = Path(os.getenv("MODEL_PATH", Path(__file__).parent / "delivery_model.pkl"))


def expected_hash(model_path: Path) -> str | None:
    """SHA-256 esperado: variável MODEL_SHA256 ou o arquivo <modelo>.sha256 gerado pelo train_model.py."""
    if os.getenv("MODEL_SHA256"):
        return os.environ["MODEL_SHA256"].strip().lower()
    hash_file = model_path.with_name(model_path.name + ".sha256")
    if hash_file.exists():
        return hash_file.read_text().split()[0].strip().lower()
    return None


def load_model(model_path: Path, sha256: str | None):
    """Carrega o modelo só se o hash bater: joblib/pickle executa código ao desserializar (issue #31)."""
    if not sha256:
        log.error("Hash do modelo não informado (MODEL_SHA256 ou %s.sha256); modelo não carregado", model_path.name)
        return None
    try:
        actual = hashlib.sha256(model_path.read_bytes()).hexdigest()
    except OSError as e:
        log.error("Modelo não encontrado em %s: %s", model_path, e)
        return None
    if not hmac.compare_digest(actual, sha256):
        log.error("Hash do modelo não confere (esperado %s, obtido %s); possível adulteração", sha256, actual)
        return None
    model = joblib.load(model_path)
    log.info("Modelo RandomForest carregado de %s (sha256 %s)", model_path, actual[:12])
    return model


model = load_model(MODEL_PATH, expected_hash(MODEL_PATH))

app = FastAPI(title="Smartroute AI Engine", version="1.0.0")
bearer = HTTPBearer(auto_error=False)


def require_service_token(credentials: HTTPAuthorizationCredentials | None = Depends(bearer)) -> None:
    if credentials is None or not hmac.compare_digest(credentials.credentials.encode(), SERVICE_TOKEN.encode()):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token de serviço inválido.",
                            headers={"WWW-Authenticate": "Bearer"})


# Input of the model
class TripData(BaseModel):
    distance: float = Field(gt=0, description="Distância em km")
    weight: float = Field(ge=0, description="Peso da carga em toneladas")
    hour_of_day: int = Field(ge=0, le=23)
    is_rainy: int = Field(ge=0, le=1)


@app.post("/predict/eta", dependencies=[Depends(require_service_token)],
          responses={401: {"description": "Token de serviço ausente ou inválido."},
                     503: {"description": "Cérebro de IA offline."}})
async def predict_eta(data: TripData):
    if model is None:
        raise HTTPException(status_code=503, detail="Cérebro de IA offline.")
    try:
        features = np.array([[data.distance, data.weight, data.hour_of_day, data.is_rainy]])

        predicted_hours = float(model.predict(features)[0])

        return {
            "predicted_arrival_hours": round(predicted_hours, 2),
            "confidence_score": 0.89, # TODO(#49): substituir por intervalo real
            "model_version": "v1.0-random-forest",
            "status": "success",
            "metadata": {
                "traffic_impact": "High" if data.hour_of_day in [7, 8, 9, 17, 18, 19] else "Normal"
            }
        }
    except Exception:
        log.exception("Erro na inferência")
        raise HTTPException(status_code=500, detail="Erro na inferência.")


@app.get("/health")
async def health_check():
    return {"status": "online", "brain": "Active" if model is not None else "Offline"}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
