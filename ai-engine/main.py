import os
from pathlib import Path

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
import joblib
import numpy as np
import uvicorn

app = FastAPI(title="Smartroute AI Engine", version="1.0.0")

# Caminho relativo a este arquivo, para não depender do diretório de execução.
# MODEL_PATH permite apontar outro artefato (sempre gerado pelo próprio pipeline: ver issue #31).
MODEL_PATH = Path(os.getenv("MODEL_PATH", Path(__file__).parent / "delivery_model.pkl"))

model = None
try:
    model = joblib.load(MODEL_PATH)
    print(f"[ALCHEMIST]: Modelo RandomForest carregado de {MODEL_PATH}.")
except Exception as e:
    print(f"[ALCHEMIST]: Falha ao carregar o modelo: Erro:{e}")

# Input of the model
class TripData(BaseModel):
    distance: float = Field(gt=0, description="Distância em km")
    weight: float = Field(ge=0, description="Peso da carga em toneladas")
    hour_of_day: int = Field(ge=0, le=23)
    is_rainy: int = Field(ge=0, le=1)

@app.post("/predict/eta", responses={503: {"description": "Cérebro de IA offline."}})
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
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Erro na inferência: {str(e)}")

@app.get("/health")
async def health_check():
    return {"status": "online", "brain": "Active" if model is not None else "Offline"}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
