"""Autenticação por token de serviço e verificação de integridade do modelo (issue #31)."""
import hashlib

import pytest

import main


def test_predict_requires_service_token(anonymous_client, trip):
    response = anonymous_client.post("/predict/eta", json=trip)

    assert response.status_code == 401
    assert response.headers["www-authenticate"] == "Bearer"


@pytest.mark.parametrize("header", ["Bearer token-errado", "Basic dXNlcjpwYXNz", "Bearer "])
def test_rejects_invalid_credentials(anonymous_client, trip, header):
    response = anonymous_client.post("/predict/eta", json=trip, headers={"Authorization": header})

    assert response.status_code in (401, 403)


def test_health_stays_public_for_orchestrators(anonymous_client):
    assert anonymous_client.get("/health").status_code == 200


def test_inference_error_does_not_leak_details(client, trip, monkeypatch):
    class Broken:
        def predict(self, _):
            raise ValueError("segredo interno /opt/modelo")

    monkeypatch.setattr(main, "model", Broken())

    response = client.post("/predict/eta", json=trip)

    assert response.status_code == 500
    assert "segredo" not in response.text


def test_bundled_model_matches_its_hash():
    assert main.model is not None
    assert main.expected_hash(main.MODEL_PATH) == hashlib.sha256(main.MODEL_PATH.read_bytes()).hexdigest()


def test_refuses_tampered_model(tmp_path):
    original = main.MODEL_PATH.read_bytes()
    tampered = tmp_path / "model.pkl"
    tampered.write_bytes(original + b"\x00")

    assert main.load_model(tampered, hashlib.sha256(original).hexdigest()) is None


def test_refuses_model_without_known_hash(tmp_path, monkeypatch):
    monkeypatch.delenv("MODEL_SHA256", raising=False)
    copy = tmp_path / "model.pkl"
    copy.write_bytes(main.MODEL_PATH.read_bytes())

    assert main.expected_hash(copy) is None
    assert main.load_model(copy, None) is None


def test_hash_can_come_from_environment(tmp_path, monkeypatch):
    copy = tmp_path / "model.pkl"
    copy.write_bytes(main.MODEL_PATH.read_bytes())
    monkeypatch.setenv("MODEL_SHA256", hashlib.sha256(copy.read_bytes()).hexdigest().upper())

    assert main.load_model(copy, main.expected_hash(copy)) is not None


def test_missing_model_file_is_not_fatal(tmp_path):
    assert main.load_model(tmp_path / "nao-existe.pkl", "0" * 64) is None
