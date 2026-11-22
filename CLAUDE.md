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
| `backend-core/` | Java 21, Spring Boot 3.3, JPA, Flyway, PostgreSQL 15 | API REST de viagens e geocodificação; orquestra a IA e persiste |
| `ai-engine/` | Python 3.12, FastAPI, scikit-learn | Inferência de ETA (`RandomForestRegressor`) |
| `smartroute-frontend/` | React 19, TypeScript, Vite, React Router, TanStack Query | Painel de despacho |

Fluxo: frontend chama `GET /api/v1/geocode` (backend → Nominatim, com cache) para origem e destino →
`POST /api/v1/trips` → backend calcula a distância (Haversine) e chama `POST /predict/eta` no ai-engine →
grava a viagem com `predictedArrival`. Se o ai-engine falhar, usa a estimativa heurística do domínio
(`eta.source = FALLBACK`).

## Comandos

```bash
# Tudo (Postgres, ai-engine, backend, Grafana). Variáveis opcionais em .env (ver .env.example)
docker compose up --build

# backend-core
cd backend-core
./mvnw test                      # todos os testes; usa PostgreSQL embarcado (zonky), sem Docker
./mvnw test -Dtest=TripTest      # um teste específico
./mvnw spring-boot:test-run      # sobe a API com PostgreSQL embarcado (dados descartados ao sair)
./mvnw spring-boot:run           # perfil dev: requer Postgres em localhost:5432
# Swagger: http://localhost:8080/swagger-ui.html · Health: /actuator/health

# ai-engine
cd ai-engine
uv pip install -r requirements.txt   # ou pip
python main.py                       # http://localhost:8000/docs
python generate_data.py && python train_model.py   # regera dataset sintético e modelo

# frontend
cd smartroute-frontend
npm ci
npm run dev                      # http://localhost:5173 (proxy de /api para localhost:8080)
npm run lint && npm run build    # build roda o typecheck (tsc -b)
```

## Arquitetura do backend (hexagonal)

Pacote base `com.logistics.logistics`:

- `domain/` — regras de negócio puras, **sem Spring/JPA**. Records imutáveis (`Trip`, `Coordinates`,
  `EtaPrediction`) que validam invariantes no construtor compacto e devolvem novas instâncias nas
  operações (`withPrediction`, `transitionTo`). `TripStatus` define a máquina de estados.
  `HeuristicEtaEstimator` é o fallback de ETA. Exceções de regra estendem `DomainException`.
- `application/` — casos de uso em `service/` (`TripService`, `GeocodingService`) e **portas de saída**
  em `port/out/` (`TripRepositoryPort`, `EtaPredictionPort`, `GeocodingPort`). Não importa nada de
  `infrastructure`. Exceções da aplicação em `exception/`.
- `infrastructure/` — adapters que implementam as portas: `persistence/` (JPA + mapper),
  `adapters/ai/` (ai-engine com timeout, retry e circuit breaker do Resilience4j),
  `adapters/geocoding/` (Nominatim, 1 req/s); `controller/` (REST, DTOs e `GlobalExceptionHandler`);
  `config/`.

Regras:
- Não exponha entidades JPA nem records de domínio na API; use DTOs (`controller/dto`).
- Erros saem como RFC 7807 (`ProblemDetail`): 400 validação (com `errors` por campo), 404 não
  encontrado, 409 regra de negócio, 503 dependência externa fora.
- Schema só muda por migration Flyway nova em `src/main/resources/db/migration` (`V<n>__descricao.sql`);
  nunca edite uma migration já mergeada. O Hibernate roda com `ddl-auto=validate`.
- Datas são `OffsetDateTime` (`timestamptz` no banco). O Jackson preserva o fuso recebido, porque a
  hora local de saída é feature do modelo (horário de pico).
- Configuração por perfil: `application.properties` (comum), `application-dev.properties` (padrões
  locais), `application-prod.properties` (sem padrões; falha se faltar variável). Nunca hardcode URLs
  ou credenciais.
- Use `Clock` injetado em vez de `now()` direto nos casos de uso (testabilidade).

## Contrato backend ↔ ai-engine

`POST /predict/eta`

```json
// request
{ "distance": 360.5, "weight": 10.0, "hour_of_day": 8, "is_rainy": 0 }
// response
{ "predicted_arrival_hours": 4.92, "confidence_score": 0.89, "model_version": "v1.0-random-forest",
  "status": "success", "metadata": { "traffic_impact": "High" } }
```

- Campos em `snake_case` no JSON; mapeados em `adapters/ai/PredictionResponse` com `@JsonProperty`.
- `predicted_arrival_hours` é **decimal**: converta para minutos (`Trip.estimateArrival`), nunca trunque.
- Modelo indisponível → `503`. Entrada inválida → `422` (validação Pydantic).
- Ao mudar o contrato, altere os dois lados no mesmo PR.

Variáveis de ambiente: `SPRING_PROFILES_ACTIVE`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD`, `AI_ENGINE_URL`, `GEOCODING_URL`, `GEOCODING_USER_AGENT`,
`GEOCODING_COUNTRY_CODES` (backend); `VITE_API_URL`, `VITE_PROXY_TARGET` (frontend).

## Frontend

- `src/api/`: cliente `fetch` tipado (`client.ts`, que lança `ApiError` com os erros por campo) e funções
  por recurso (`trips.ts`). Componentes nunca chamam `fetch` direto.
- Dados do servidor via TanStack Query (chaves em `tripKeys`); estado de filtro/página na URL.
- Rotas em `main.tsx`; páginas em `src/pages/`, componentes reutilizáveis em `src/components/`.
- Datas enviadas com o fuso do navegador (`localInputToOffsetIso`).

## Convenções

- **Idioma:** código (identificadores) em inglês; mensagens ao usuário, issues, PRs e docs em português.
- **Branches:** `tipo/descricao-curta`: `feat/`, `fix/`, `refactor/`, `test/`, `security/`, `chore/`, `docs/`.
- **Commits:** Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`),
  referenciando a issue (`Closes #N`).
- **PRs:** um PR por fase/etapa do roadmap, descrevendo como foi testado.
- Correção de bug vem com teste que falharia antes da correção sempre que possível.
- Java: Lombok só em entidades JPA; prefira `record` para DTOs e domínio.
- Python: tipagem com Pydantic `Field` para validar entradas.

## Segurança (regras não negociáveis)

- Nunca commitar segredos, `.env`, dumps de banco ou dados pessoais reais. As credenciais padrão do
  `docker-compose.yml` são apenas de desenvolvimento (issue #30).
- Dados de localização de motoristas/clientes são dados pessoais (LGPD): não logar coordenadas
  associadas a pessoas, minimizar retenção.
- Artefatos de modelo `.pkl` executam código ao carregar: só carregue modelos gerados pelo próprio
  pipeline (issue #31 migra para formato seguro).
- Containers devem rodar como usuário não-root.
- Ações de agentes de IA que alteram dados exigem confirmação humana e respeitam o tenant do usuário.

## Estado conhecido / dívidas

- Sem autenticação; CORS liberado para qualquer origem (`*`): Fase 3 (#28, #29).
- `is_rainy` sempre 0 e `confidence_score` fixo: issues #47 e #49.
- Distância em linha reta, não por estrada: issue #45.
- Dataset de treino é sintético (`generate_data.py`).
- Sem CI ainda: issue #27.
