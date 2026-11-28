"""Configuração lida do ambiente. Sem SERVICE_TOKEN o serviço não sobe (fail closed, ETA-010)."""
import os
from dataclasses import dataclass
from pathlib import Path

DEFAULT_MODEL_PATH = Path(__file__).resolve().parent.parent / "models" / "delivery_model.pkl"


@dataclass(frozen=True)
class Settings:
    service_token: str
    model_path: Path
    model_sha256: str | None  # None: usa o arquivo <modelo>.sha256 gerado pelo treino

    @classmethod
    def from_env(cls) -> "Settings":
        token = os.getenv("SERVICE_TOKEN", "")
        if not token:
            raise RuntimeError("SERVICE_TOKEN não definido: o ai-engine não aceita chamadas sem autenticação")
        sha256 = os.getenv("MODEL_SHA256", "").strip().lower() or None
        return cls(token, Path(os.getenv("MODEL_PATH", DEFAULT_MODEL_PATH)), sha256)
