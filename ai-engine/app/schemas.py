"""Entrada e saída de /predict/eta. Os campos são o contrato com o backend-core (contracts/ai-engine)."""
from typing import Literal

from pydantic import BaseModel, Field


class TripData(BaseModel):
    distance: float = Field(gt=0, description="Distância em km")
    weight: float = Field(ge=0, description="Peso da carga em toneladas")
    hour_of_day: int = Field(ge=0, le=23, description="Hora local de saída")
    is_rainy: int = Field(ge=0, le=1, description="1 se chove na saída")


class EtaMetadata(BaseModel):
    traffic_impact: Literal["High", "Normal"]


class EtaResponse(BaseModel):
    predicted_arrival_hours: float = Field(description="Duração prevista em horas decimais (não truncar)")
    confidence_score: float = Field(ge=0, le=1)
    model_version: str
    status: Literal["success"]
    metadata: EtaMetadata


class Health(BaseModel):
    status: Literal["online"]
    brain: Literal["Active", "Offline"]
