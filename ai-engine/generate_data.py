import pandas as pd
import numpy as np

def generate_logistic_dataset(n_samples=5000):
    rng = np.random.default_rng(42)

    distance = rng.uniform(10, 500, n_samples) # km
    weight = rng.uniform(1, 30, n_samples)     # toneladas
    hour_of_day = rng.randint(0, 24, n_samples) # Horas do dia
    is_rainy = rng.choice([0, 1], n_samples, p=[0.7, 0.3]) # 30% chuva

    base_time = distance / 80

    weight_delay = (weight * 0.005) * base_time

    traffic_multiplier = np.where(
        ((hour_of_day >= 7) & (hour_of_day <= 9)) |
        ((hour_of_day >= 17) & (hour_of_day <= 19)),
        1.5, 1.0
    )

    weather_multiplier = np.where(is_rainy == 1, 1.2, 1.0)

    real_time = (base_time + weight_delay) * traffic_multiplier * weather_multiplier
    real_time += rng.normal(0, 0.2, n_samples) # Ruído de 12 min

    df = pd.DataFrame({
        'distance': distance,
        'weight': weight,
        'hour_of_day': hour_of_day,
        'is_rainy': is_rainy,
        'delivery_time_hours': real_time
    })

    df.to_csv('logistic_data.csv', index=False)
    print(f"Dataset com {n_samples} viagens gerado com sucesso!")

if __name__ == "__main__":
    generate_logistic_dataset()
