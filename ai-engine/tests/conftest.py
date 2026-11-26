import os

# Precisa existir antes de importar main: sem token o serviço se recusa a subir
os.environ.setdefault("SERVICE_TOKEN", "test-service-token")

import pytest  # noqa: E402
from fastapi.testclient import TestClient  # noqa: E402

import main  # noqa: E402

AUTH = {"Authorization": f"Bearer {os.environ['SERVICE_TOKEN']}"}


@pytest.fixture
def client():
    """Cliente autenticado, como o backend-core."""
    return TestClient(main.app, headers=AUTH)


@pytest.fixture
def anonymous_client():
    return TestClient(main.app)


@pytest.fixture
def trip():
    return {"distance": 360.5, "weight": 10.0, "hour_of_day": 12, "is_rainy": 0}
