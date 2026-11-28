"""Caminhos do pipeline de treino, relativos à pasta ai-engine (independem do diretório atual)."""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATASET = ROOT / "data" / "logistic_data.csv"
MODEL = ROOT / "models" / "delivery_model.pkl"
