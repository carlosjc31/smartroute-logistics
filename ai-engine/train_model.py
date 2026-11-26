import hashlib
from pathlib import Path
import pandas as pd
import joblib
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_absolute_error

def train():

    df = pd.read_csv('logistic_data.csv')
    X = df.drop('delivery_time_hours', axis=1)
    y = df['delivery_time_hours']

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    print("Treinando o modelo de predição...")
    model = RandomForestRegressor(n_estimators=100, random_state=42, min_samples_leaf=2, max_features='sqrt')
    model.fit(X_train, y_train)

    preds = model.predict(X_test)
    error = mean_absolute_error(y_test, preds)
    print(f"Erro Médio: {round(error * 60, 2)} minutos.")

    joblib.dump(model, 'delivery_model.pkl')
    # O main.py só carrega o modelo se o hash conferir (proteção contra artefato adulterado, issue #31)
    digest = hashlib.sha256(Path('delivery_model.pkl').read_bytes()).hexdigest()
    Path('delivery_model.pkl.sha256').write_text(f"{digest}  delivery_model.pkl\n")
    print(f"Modelo salvo como 'delivery_model.pkl' (sha256 {digest})")

if __name__ == "__main__":
    train()

