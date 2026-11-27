import pytest

import main


# @req ETA-004
def test_health_reports_model_loaded(client):
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "online", "brain": "Active"}


# @req ETA-001
def test_predicts_positive_duration(client, trip):
    response = client.post("/predict/eta", json=trip)

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "success"
    assert body["model_version"] == "v1.0-random-forest"
    assert 0 < body["predicted_arrival_hours"] < 24
    assert 0 <= body["confidence_score"] <= 1


# @req ETA-002
@pytest.mark.parametrize("hour, impact", [(6, "Normal"), (8, "High"), (12, "Normal"), (18, "High"), (20, "Normal")])
def test_flags_traffic_impact_at_peak_hours(client, trip, hour, impact):
    response = client.post("/predict/eta", json={**trip, "hour_of_day": hour})

    assert response.json()["metadata"] == {"traffic_impact": impact}


# @req ETA-002
def test_peak_hour_takes_longer(client, trip):
    off_peak = client.post("/predict/eta", json={**trip, "hour_of_day": 12}).json()
    peak = client.post("/predict/eta", json={**trip, "hour_of_day": 8}).json()

    assert peak["predicted_arrival_hours"] > off_peak["predicted_arrival_hours"]


# @req ETA-003
@pytest.mark.parametrize(
    "field, value",
    [
        ("distance", 0),
        ("distance", -10),
        ("weight", -1),
        ("hour_of_day", 24),
        ("hour_of_day", -1),
        ("is_rainy", 2),
        ("distance", "longe"),
    ],
)
def test_rejects_invalid_input(client, trip, field, value):
    response = client.post("/predict/eta", json={**trip, field: value})

    assert response.status_code == 422
    assert response.json()["detail"][0]["loc"] == ["body", field]


# @req ETA-003
def test_rejects_missing_field(client, trip):
    del trip["weight"]

    response = client.post("/predict/eta", json=trip)

    assert response.status_code == 422


# @req ETA-004
def test_returns_503_without_model(client, trip, monkeypatch):
    monkeypatch.setattr(main, "model", None)

    assert client.post("/predict/eta", json=trip).status_code == 503
    assert client.get("/health").json()["brain"] == "Offline"
