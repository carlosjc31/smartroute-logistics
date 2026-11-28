import os

# Precisa existir antes de importar app.main: sem token o serviço se recusa a subir
os.environ.setdefault("SERVICE_TOKEN", "test-service-token")

import pytest  # noqa: E402
from fastapi.testclient import TestClient  # noqa: E402

from app.main import app as application  # noqa: E402

AUTH = {"Authorization": f"Bearer {os.environ['SERVICE_TOKEN']}"}


@pytest.fixture
def client():
    """Cliente autenticado, como o backend-core."""
    return TestClient(application, headers=AUTH)


@pytest.fixture
def anonymous_client():
    return TestClient(application)


@pytest.fixture
def replace_model():
    """Troca o modelo carregado durante o teste e restaura no fim.

    Não use monkeypatch em app.state: o State do Starlette guarda os valores em _state, e o monkeypatch
    remove o atributo ao desfazer em vez de restaurá-lo.
    """
    original = application.state.model

    def replace(model):
        application.state.model = model

    yield replace
    application.state.model = original


@pytest.fixture
def trip():
    return {"distance": 360.5, "weight": 10.0, "hour_of_day": 12, "is_rainy": 0}
