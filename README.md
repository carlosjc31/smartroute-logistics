# 🚚 SmartRoute Logistics

Sistema de logística que **prevê o tempo de entrega (ETA)** de uma viagem com machine learning.
O usuário informa origem, destino e carga; o backend consulta um serviço de IA e grava a viagem com a
chegada prevista.

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring](https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Python](https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-009688?style=for-the-badge&logo=fastapi&logoColor=white)
![React](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

## 🏗️ Arquitetura

```
┌──────────────┐   POST /api/v1/trips   ┌──────────────────┐  POST /predict/eta  ┌──────────────────┐
│   Frontend   │ ─────────────────────▶ │   backend-core   │ ──────────────────▶ │    ai-engine     │
│ React + Vite │                        │ Java 21 · Spring │                     │ Python · FastAPI │
└──────┬───────┘                        └────────┬─────────┘                     │  RandomForest    │
       │ geocoding                               │ JPA                           └──────────────────┘
       ▼                                         ▼
 OpenStreetMap (Nominatim)               PostgreSQL 15
```

- **frontend:** converte o nome das cidades em coordenadas (OpenStreetMap) e cria a viagem.
- **backend-core:** organizado em camadas `domain` / `application` / `infrastructure` (arquitetura
  hexagonal). Um *adapter* chama o serviço de IA e o resultado define a chegada prevista.
- **ai-engine:** API FastAPI que carrega um `RandomForestRegressor` (scikit-learn) e responde ao
  endpoint `/predict/eta`, além de `/health`.

## 🧠 O modelo

Treinado com um dataset sintético de 5.000 viagens (`generate_data.py`), considerando:

| Variável | Efeito no tempo |
|---|---|
| Distância (km) | base, a 80 km/h |
| Peso da carga (t) | atraso proporcional |
| Hora do dia | ×1,5 nos horários de pico (7–9h e 17–19h) |
| Chuva | ×1,2 |

O treino (`train_model.py`) separa 20% para teste e reporta o erro médio absoluto em minutos.

## 🏃 Como executar

Pré-requisito: Docker e Docker Compose.

```bash
git clone https://github.com/carlosjc31/smartroute-logistics.git
cd smartroute-logistics
docker compose up --build
```

| Serviço | URL |
|---|---|
| backend-core | http://localhost:8080/api/v1/trips |
| ai-engine | http://localhost:8000/docs |

Frontend:
```bash
cd smartroute-frontend
npm install
npm run dev
```

Retreinar o modelo:
```bash
cd ai-engine
python generate_data.py && python train_model.py
```

> As credenciais do `docker-compose.yml` são apenas para ambiente local.

## 👤 Autor

**José Carlos Carneiro** — [LinkedIn](https://www.linkedin.com/in/jose-carlos-carneiro/)
