"""Contrato backend ↔ ai-engine: o OpenAPI versionado e os exemplos verificados também pelo AiEngineContractTest (Java)."""
import json
from pathlib import Path

from app.contract import CONTRACT_PATH, normalize
from app.main import app as application
from app.schemas import TripData

CONTRACTS = Path(__file__).resolve().parents[2] / "contracts" / "ai-engine"


def load(name):
    return json.loads((CONTRACTS / name).read_text(encoding="utf-8"))


def json_type(value):
    if isinstance(value, bool):
        return "boolean"
    if isinstance(value, (int, float)):
        return "number"
    return type(value).__name__


# @req ETA-001, ETA-005
def test_request_sent_by_backend_is_accepted(client):
    request = load("predict-eta.request.json")

    assert set(TripData.model_fields) == set(request), "campos de entrada divergem do contrato"
    assert client.post("/predict/eta", json=request).status_code == 200


# @req ETA-001, ETA-005
def test_response_matches_what_backend_reads(client):
    expected = load("predict-eta.response.json")

    actual = client.post("/predict/eta", json=load("predict-eta.request.json")).json()

    assert set(actual) == set(expected), "campos de resposta divergem do contrato"
    for field, value in expected.items():
        assert json_type(actual[field]) == json_type(value), field
    assert set(actual["metadata"]) == set(expected["metadata"])


# @req ETA-005
def test_openapi_matches_versioned_contract():
    versioned = json.loads(CONTRACT_PATH.read_text(encoding="utf-8"))

    assert normalize(application.openapi()) == normalize(versioned), (
        "o código diverge de contracts/ai-engine/openapi.json: rode `python -m app.contract` e revise o diff"
    )
