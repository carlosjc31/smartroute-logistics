"""Aplicação FastAPI. `app` é criado na importação: sem SERVICE_TOKEN a importação falha e o serviço não sobe."""
import logging

import numpy as np
from fastapi import Depends, FastAPI, HTTPException, Request

from app.config import Settings
from app.model import expected_hash, load_model
from app.schemas import EtaMetadata, EtaResponse, Health, TripData
from app.security import require_service_token

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
log = logging.getLogger("ai-engine")

MODEL_VERSION = "v1.0-random-forest"
PEAK_HOURS = {7, 8, 9, 17, 18, 19}


def create_app(settings: Settings) -> FastAPI:
    application = FastAPI(title="Smartroute AI Engine", version="1.0.0")
    application.state.settings = settings
    application.state.model = load_model(settings.model_path, expected_hash(settings.model_path, settings.model_sha256))

    @application.post("/predict/eta", response_model=EtaResponse, dependencies=[Depends(require_service_token)],
                      responses={401: {"description": "Token de serviço ausente ou inválido."},
                                 500: {"description": "Erro na inferência (sem detalhes internos)."},
                                 503: {"description": "Cérebro de IA offline."}})
    async def predict_eta(data: TripData, request: Request) -> EtaResponse:
        model = request.app.state.model
        if model is None:
            raise HTTPException(status_code=503, detail="Cérebro de IA offline.")
        try:
            features = np.array([[data.distance, data.weight, data.hour_of_day, data.is_rainy]])
            predicted_hours = float(model.predict(features)[0])
        except Exception:
            log.exception("Erro na inferência")
            raise HTTPException(status_code=500, detail="Erro na inferência.")
        return EtaResponse(
            predicted_arrival_hours=round(predicted_hours, 2),
            confidence_score=0.89,  # TODO(#49): substituir por intervalo real
            model_version=MODEL_VERSION,
            status="success",
            metadata=EtaMetadata(traffic_impact="High" if data.hour_of_day in PEAK_HOURS else "Normal"),
        )

    @application.get("/health", response_model=Health)
    async def health_check(request: Request) -> Health:
        return Health(status="online", brain="Active" if request.app.state.model is not None else "Offline")

    return application


app = create_app(Settings.from_env())
