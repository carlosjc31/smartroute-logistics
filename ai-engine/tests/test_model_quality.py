"""Qualidade mínima do artefato versionado, medida em viagens que o modelo nunca viu."""
import numpy as np

import main
from generate_data import build_dataset

# Erro médio absoluto máximo aceito, em horas (30 min). O ruído do dataset sozinho dá cerca de 10 min.
MAX_MAE_HOURS = 0.5


# @req ETA-020
def test_model_error_below_threshold():
    holdout = build_dataset(n_samples=1000, seed=2024)  # semente diferente da usada no treino (42)
    features = holdout.drop(columns="delivery_time_hours")

    predictions = main.model.predict(features)
    mae = float(np.mean(np.abs(predictions - holdout["delivery_time_hours"])))

    assert mae < MAX_MAE_HOURS, f"MAE de {mae * 60:.1f} min acima do limite de {MAX_MAE_HOURS * 60:.0f} min"
