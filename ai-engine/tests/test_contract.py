"""Contrato backend ↔ ai-engine: os mesmos exemplos são verificados pelo AiEngineContractTest (Java)."""
import json
from pathlib import Path

import main

CONTRACTS = Path(__file__).resolve().parents[2] / "contracts" / "ai-engine"


def load(name):
    return json.loads((CONTRACTS / name).read_text(encoding="utf-8"))


def json_type(value):
    if isinstance(value, bool):
        return "boolean"
    if isinstance(value, (int, float)):
        return "number"
    return type(value).__name__


def test_request_sent_by_backend_is_accepted(client):
    request = load("predict-eta.request.json")

    assert set(main.TripData.model_fields) == set(request), "campos de entrada divergem do contrato"
    assert client.post("/predict/eta", json=request).status_code == 200


def test_response_matches_what_backend_reads(client):
    expected = load("predict-eta.response.json")

    actual = client.post("/predict/eta", json=load("predict-eta.request.json")).json()

    assert set(actual) == set(expected), "campos de resposta divergem do contrato"
    for field, value in expected.items():
        assert json_type(actual[field]) == json_type(value), field
    assert set(actual["metadata"]) == set(expected["metadata"])
