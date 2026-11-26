import pytest
from fastapi.testclient import TestClient

import main


@pytest.fixture
def client():
    return TestClient(main.app)


@pytest.fixture
def trip():
    return {"distance": 360.5, "weight": 10.0, "hour_of_day": 12, "is_rainy": 0}
