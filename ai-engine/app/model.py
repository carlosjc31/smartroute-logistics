"""Carga do modelo com hash conferido: joblib/pickle executa código ao desserializar (ETA-011, issue #31)."""
import hashlib
import hmac
import logging
from pathlib import Path

import joblib

log = logging.getLogger("ai-engine")


def expected_hash(model_path: Path, configured: str | None = None) -> str | None:
    """SHA-256 esperado: o configurado (MODEL_SHA256) ou o arquivo <modelo>.sha256 gerado pelo treino."""
    if configured:
        return configured.strip().lower()
    hash_file = model_path.with_name(model_path.name + ".sha256")
    if hash_file.exists():
        return hash_file.read_text().split()[0].strip().lower()
    return None


def load_model(model_path: Path, sha256: str | None):
    """Devolve o modelo, ou None se o hash faltar, não conferir ou o arquivo não existir (ETA-004)."""
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
