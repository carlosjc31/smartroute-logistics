"""Autenticação por token de serviço e verificação de integridade do modelo (issue #31)."""
import hashlib
import os
import subprocess
import sys
from pathlib import Path

import pytest

from app.main import app as application
from app.config import Settings
from app.model import expected_hash, load_model

AI_ENGINE_DIR = Path(__file__).resolve().parent.parent
MODEL_PATH = application.state.settings.model_path


# @req ETA-010
def test_refuses_to_start_without_service_token():
    env = {key: value for key, value in os.environ.items() if key != "SERVICE_TOKEN"}
    result = subprocess.run(
        [sys.executable, "-c", "import app.main"],
        cwd=AI_ENGINE_DIR, env=env, capture_output=True, text=True, timeout=120,
    )

    assert result.returncode != 0
    assert "SERVICE_TOKEN" in result.stderr


# @req ETA-010
def test_predict_requires_service_token(anonymous_client, trip):
    response = anonymous_client.post("/predict/eta", json=trip)

    assert response.status_code == 401
    assert response.headers["www-authenticate"] == "Bearer"


# @req ETA-010
@pytest.mark.parametrize("header", ["Bearer token-errado", "Basic dXNlcjpwYXNz", "Bearer "])
def test_rejects_invalid_credentials(anonymous_client, trip, header):
    response = anonymous_client.post("/predict/eta", json=trip, headers={"Authorization": header})

    assert response.status_code in (401, 403)


# @req ETA-010
def test_health_stays_public_for_orchestrators(anonymous_client):
    assert anonymous_client.get("/health").status_code == 200


# @req ETA-012
def test_inference_error_does_not_leak_details(client, trip, replace_model):
    class Broken:
        def predict(self, _):
            raise ValueError("segredo interno /opt/modelo")

    replace_model(Broken())

    response = client.post("/predict/eta", json=trip)

    assert response.status_code == 500
    assert "segredo" not in response.text


# @req ETA-011
def test_bundled_model_matches_its_hash():
    assert application.state.model is not None
    assert expected_hash(MODEL_PATH) == hashlib.sha256(MODEL_PATH.read_bytes()).hexdigest()


# @req ETA-011
def test_refuses_tampered_model(tmp_path):
    original = MODEL_PATH.read_bytes()
    tampered = tmp_path / "model.pkl"
    tampered.write_bytes(original + b"\x00")

    assert load_model(tampered, hashlib.sha256(original).hexdigest()) is None


# @req ETA-011
def test_refuses_model_without_known_hash(tmp_path):
    copy = tmp_path / "model.pkl"
    copy.write_bytes(MODEL_PATH.read_bytes())

    assert expected_hash(copy) is None
    assert load_model(copy, None) is None


# @req ETA-011
def test_hash_can_come_from_environment(tmp_path, monkeypatch):
    copy = tmp_path / "model.pkl"
    copy.write_bytes(MODEL_PATH.read_bytes())
    monkeypatch.setenv("SERVICE_TOKEN", "x")
    monkeypatch.setenv("MODEL_PATH", str(copy))
    monkeypatch.setenv("MODEL_SHA256", hashlib.sha256(copy.read_bytes()).hexdigest().upper())

    settings = Settings.from_env()

    assert settings.model_path == copy
    assert load_model(copy, expected_hash(copy, settings.model_sha256)) is not None


# @req ETA-004
def test_missing_model_file_is_not_fatal(tmp_path):
    assert load_model(tmp_path / "nao-existe.pkl", "0" * 64) is None
