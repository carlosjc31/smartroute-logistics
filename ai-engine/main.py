from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import joblib
import numpy as np
import uvicorn

app = FastAPI(title="Smartroute AI Engine", version="1.0.0")

# Running the model
try:
    model = joblib.load("delivery_model.pkl")
    print("[ALCHEMIST]: Modelo RandomForest carregado e operacional.")
except Exception as e:
    print(f"[ALCHEMIST]: Falha ao carregar o modelo: Erro:{e}")

# Input of the model
class TripData(BaseModel):
    distance: float
    weight: float
    hour_of_day: int
    is_rainy: int

# Resposta simulada (Mock) para o primeiro teste
@app.post("/predict/eta")
async def predict_eta(data: TripData):
    if model is None:
        raise HTTPException(status_code=500, detail="Cérebro de IA offline.")
    try:
        features = np.array([[data.distance, data.weight, data.hour_of_day, data.is_rainy]])

        predicted_hours = model.predict(features)[0]

        return {
            "predicted_arrival_hours": round(predicted_hours, 2),
            "confidence_score": 0.89, # Score base para o primeiro teste
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
    return {"status": "online", "brain": "Active" if model else "Offline"}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
