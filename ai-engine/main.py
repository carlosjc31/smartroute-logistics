from fastapi import FastAPI
from pydantic import BaseModel
from datetime import datetime
import random

app = FastAPI(title="SmartRoute AI Engine")

class TripData(BaseModel):
    origin_lat: float
    origin_lng: float
    dest_lat: float
    dest_lng: float
    payload_weight: float
    departure_time: datetime

@app.post("/predict-eta")
async def predict_eta(data: TripData):
    # Mock do modelo de ML: Cálculo baseado em distância básica + peso
    base_hours = random.uniform(2, 5)
    weight_factor = data.payload_weight / 1000  # Atraso por tonelada
    prediction = base_hours + weight_factor

    return {
        "predicted_arrival": prediction,
        "confidence_score": 0.92,
        "model_version": "v1.0.0-mock"
    }
