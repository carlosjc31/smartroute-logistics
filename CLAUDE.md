# CLAUDE.md

Guia para agentes de IA (Claude Code) e pessoas desenvolvedoras que trabalham neste repositório.

## Visão geral

SmartRoute Logistics é um sistema de despacho de cargas que **prevê o horário de chegada (ETA)** de
uma viagem com machine learning. O objetivo é evoluir para uma plataforma para transportadoras: multi-tenant,
com frota, motoristas, roteirização e agentes de IA. O roadmap está nas issues/milestones do GitHub
(Fase 0 a Fase 7).

Monorepo com três serviços:

| Pasta | Stack | Responsabilidade |
|---|---|---|
| `backend-core/` | Java 21, Spring Boot 3.3, JPA, PostgreSQL 15 | API REST de viagens; orquestra a IA e persiste |
| `ai-engine/` | Python 3.12, FastAPI, scikit-learn | Inferência de ETA (`RandomForestRegressor`) |
| `smartroute-frontend/` | React 19, Vite, axios | Painel de despacho |

Fluxo: frontend geocodifica as cidades (Nominatim) → `POST /api/v1/trips` no backend → backend calcula a
distância (Haversine) e chama `POST /predict/eta` no ai-engine → grava a viagem com `predictedArrival`.

## Comandos

```bash
# Tudo (Postgres, ai-engine, backend, Grafana)
docker compose up --build

# backend-core
cd backend-core
./mvnw test                      # testes (TripTest roda sem banco; contextLoads exige Postgres)
./mvnw test -Dtest=TripTest      # só testes de domínio
./mvnw spring-boot:run           # requer Postgres e ai-engine em localhost

# ai-engine
cd ai-engine
uv pip install -r requirements.txt   # ou pip
python main.py                       # http://localhost:8000/docs
python generate_data.py && python train_model.py   # regera dataset sintético e modelo

# frontend
cd smartroute-frontend
npm install                      # se falhar por peer deps do eslint, use --legacy-peer-deps
npm run dev                      # http://localhost:5173
npm run lint && npm run build
```

## Arquitetura do backend (hexagonal)

Pacote base `com.logistics.logistics`:

- `domain/` — regras de negócio puras, **sem Spring/JPA**. Records imutáveis (`Trip`, `Coordinates`)
  que validam invariantes no construtor compacto. Cálculos de domínio (distância, ETA) ficam aqui.
- `application/` — casos de uso (`services/`). Deve depender de **portas** (interfaces) e não de
  classes de `infrastructure` (ver issue #12; ainda não está assim).
- `infrastructure/` — adapters: `controller/` (REST + DTOs), `persistence/` (entidades JPA e
  repositórios), `adapters/` (cliente HTTP do ai-engine), `config/`.

Regras:
- Não exponha entidades JPA na API; use DTOs (`controller/dto`).
- Um único enum de status: `Trip.TripStatus` (domínio), reutilizado pela entidade.
- Validação de entrada com Bean Validation nos DTOs; invariantes de negócio nos records de domínio.
- Configuração via `application.properties` com placeholders de variáveis de ambiente
  (`${VAR:default}`). Nunca hardcode URLs ou credenciais.

## Contrato backend ↔ ai-engine

`POST /predict/eta`

```json
// request
{ "distance": 360.5, "weight": 10.0, "hour_of_day": 8, "is_rainy": 0 }
// response
{ "predicted_arrival_hours": 4.92, "confidence_score": 0.89, "model_version": "v1.0-random-forest",
  "status": "success", "metadata": { "traffic_impact": "High" } }
```

- Campos em `snake_case` no JSON; mapeados em `PredictionResponse` com `@JsonProperty`.
- `predicted_arrival_hours` é **decimal** — converta para minutos (`Trip.estimateArrival`), nunca trunque.
- Modelo indisponível → `503`. Entrada inválida → `422` (validação Pydantic).
- Ao mudar o contrato, altere os dois lados no mesmo PR.

Variáveis de ambiente relevantes: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD`, `AI_ENGINE_URL` (backend); `VITE_API_URL` (frontend, ver `.env.example`).

## Convenções

- **Idioma:** código (identificadores) em inglês; mensagens ao usuário, issues, PRs e docs em português.
- **Branches:** `tipo/descricao-curta` — `feat/`, `fix/`, `refactor/`, `test/`, `security/`, `chore/`, `docs/`.
- **Commits:** Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`),
  referenciando a issue (`Closes #N`).
- **PRs:** um PR por fase/etapa do roadmap, sempre a partir de `main`, descrevendo como foi testado.
- Correção de bug vem com teste que falharia antes da correção sempre que possível.
- Java: Lombok permitido em entidades JPA; prefira `record` para DTOs e domínio.
- Python: tipagem com Pydantic `Field` para validar entradas.

## Segurança (regras não negociáveis)

- Nunca commitar segredos, `.env`, dumps de banco ou dados pessoais reais. As credenciais do
  `docker-compose.yml` são apenas de desenvolvimento (issue #30 remove).
- Dados de localização de motoristas/clientes são dados pessoais (LGPD): não logar coordenadas
  associadas a pessoas, minimizar retenção.
- Artefatos de modelo `.pkl` executam código ao carregar: só carregue modelos gerados pelo próprio
  pipeline (issue #31 migra para formato seguro).
- Containers devem rodar como usuário não-root.
- Ações de agentes de IA que alteram dados exigem confirmação humana e respeitam o tenant do usuário.

## Estado conhecido / dívidas

- Sem autenticação; CORS liberado (`*`) — Fase 3.
- `ddl-auto=update` sem migrations — issue #14.
- `is_rainy` sempre 0 e `confidence_score` fixo — issues #47 e #49.
- Dataset de treino é sintético (`generate_data.py`).
- `contextLoads` precisa de Postgres; Testcontainers na issue #22.
