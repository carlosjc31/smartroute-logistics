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
| `backend-core/` | Java 21, Spring Boot 3.5, JPA, Flyway, PostgreSQL 15 | API REST de viagens e geocodificação; orquestra a IA e persiste |
| `ai-engine/` | Python 3.12, FastAPI, scikit-learn | Inferência de ETA (`RandomForestRegressor`) |
| `smartroute-frontend/` | React 19, TypeScript, Vite, React Router, TanStack Query | Painel de despacho |

Fluxo: o usuário entra pelo Keycloak (OIDC + PKCE) e o frontend envia o JWT em toda chamada. O frontend chama `GET /api/v1/geocode` (backend → Nominatim, com cache) para origem e destino →
`POST /api/v1/trips` → backend calcula a distância (Haversine) e chama `POST /predict/eta` no ai-engine →
grava a viagem com `predictedArrival` e um registro em `audit_log`. Se o ai-engine falhar, usa a
estimativa heurística do domínio (`eta.source = FALLBACK`).

## Spec-Driven Development (leia antes de mudar comportamento)

- Toda funcionalidade nova ou mudança de comportamento começa por uma spec em `specs/NNN-nome/`
  (`spec.md` → aprovação do mantenedor → `plan.md` + `tasks.md` → código). Fluxo e status em
  `specs/README.md`; princípios não negociáveis em `specs/constitution.md`.
- Não implemente sem spec `approved`. Se a spec não cobre o caso ou o contradiz, pare e proponha a
  alteração da spec em vez de divergir.
- Skills do projeto: `/spec-new` (escreve a spec), `/spec-plan` (plano e tasks de uma spec aprovada),
  `/spec-implement` (executa as tasks). Subagente `spec-reviewer` revisa spec, plano ou diff contra a
  constituição antes da aprovação ou do PR.
- Todo teste que cobre um requisito leva `@req ID` em comentário (`// @req TRIP-004`, `# @req ETA-001`).
  `python3 scripts/check_spec_traceability.py` (também no CI) falha se uma spec `implemented` tiver
  requisito sem teste ou se um `@req` apontar para requisito inexistente; `--report` mostra o mapa.
- Decisões de arquitetura ficam em `docs/adr/` (formato em `docs/adr/template.md`). Leia as ADRs antes
  de propor mudança estrutural; decisão nova vira ADR nova, a antiga é marcada como substituída.
- Reestruturação em andamento antes da Fase 4: R1 (SDD e ADRs), R2 (API contract-first, ADR 0009),
  R3 (monólito modular com Spring Modulith, ADR 0004), R4 (ai-engine em pacote e frontend por
  funcionalidade).

## Comandos

```bash
# Tudo (Postgres, Keycloak, ai-engine, backend, Grafana). Exige .env (cp .env.example .env) com os segredos
docker compose up --build

# backend-core
cd backend-core
./mvnw test                      # unitários + contrato + PostgreSQL embarcado (zonky), sem Docker
./mvnw test -Dtest=TripTest      # um teste específico
./mvnw verify                    # + integração *IT (Testcontainers/WireMock, exige Docker) + JaCoCo ≥ 80%
./mvnw spring-boot:test-run      # API com PostgreSQL embarcado; login exige `docker compose up -d keycloak`
./mvnw spring-boot:run           # perfil dev: requer Postgres em localhost:5432
# Swagger: http://localhost:8080/swagger-ui.html · Health: /actuator/health

# ai-engine
cd ai-engine
uv pip install -r requirements.txt   # ou pip
SERVICE_TOKEN=dev-local-token python main.py   # http://localhost:8000/docs (sem token não sobe)
pip install -r requirements-dev.txt && pytest   # testes (API, contrato, qualidade do modelo)
python generate_data.py && python train_model.py   # regera dataset sintético e modelo

# specs
python3 scripts/check_spec_traceability.py   # requisitos ↔ testes (@req); --report para o mapa

# frontend
cd smartroute-frontend
npm ci
npm run dev                      # http://localhost:5173 (proxy de /api para localhost:8080)
npm test                         # Vitest + Testing Library
npm run lint && npm run build    # build roda o typecheck (tsc -b)
E2E_PASSWORD=... npm run e2e     # Playwright; exige o compose no ar (login real no Keycloak)
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
  `adapters/geocoding/` (Nominatim, 1 req/s); `persistence/AuditLogJdbcAdapter` (auditoria, só INSERT);
  `controller/` (REST, DTOs e `GlobalExceptionHandler`); `security/` (JWT do Keycloak, perfis, CORS,
  headers, rate limit); `config/`.

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
- Toda rota nova precisa de regra explícita em `SecurityConfig` (o padrão é `denyAll`) e de teste de
  acesso em `ApiSecurityTest`. Perfis em `security/Roles`; a aplicação não importa Spring Security, e o
  ator vem da porta `CurrentActorPort`.
- Toda alteração de dado de negócio passa por `AuditService` na mesma transação. Os estados auditados
  nunca incluem coordenadas nem endereços.

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
- Modelo indisponível → `503`. Entrada inválida → `422` (validação Pydantic). Sem token → `401`.
- Autenticação: `Authorization: Bearer <AI_ENGINE_TOKEN>` (no ai-engine, `SERVICE_TOKEN`). O modelo só
  carrega se o SHA-256 bater com `delivery_model.pkl.sha256` (regerado pelo `train_model.py`).
- Ao mudar o contrato, altere os dois lados no mesmo PR. Os exemplos canônicos ficam em
  `contracts/ai-engine/` e são testados pelos dois serviços (`AiEngineContractTest` e
  `tests/test_contract.py`): atualize-os junto.

Variáveis de ambiente: `SPRING_PROFILES_ACTIVE`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD`, `AI_ENGINE_URL`, `AI_ENGINE_TOKEN`, `OIDC_ISSUER_URI`, `OIDC_JWK_SET_URI`,
`OIDC_AUDIENCE`, `CORS_ALLOWED_ORIGINS`, `RATE_LIMIT_CAPACITY`, `GEOCODING_URL`, `GEOCODING_USER_AGENT`,
`GEOCODING_COUNTRY_CODES` (backend); `SERVICE_TOKEN`, `MODEL_PATH`, `MODEL_SHA256` (ai-engine);
`VITE_API_URL`, `VITE_PROXY_TARGET`, `VITE_OIDC_AUTHORITY`, `VITE_OIDC_CLIENT_ID` (frontend).

## Frontend

- `src/api/`: cliente `fetch` tipado (`client.ts`, que lança `ApiError` com os erros por campo) e funções
  por recurso (`trips.ts`). Componentes nunca chamam `fetch` direto.
- Dados do servidor via TanStack Query (chaves em `tripKeys`); estado de filtro/página na URL.
- Rotas em `main.tsx`; páginas em `src/pages/`, componentes reutilizáveis em `src/components/`.
- Autenticação em `src/auth/` (`react-oidc-context`): `RequireAuth` protege o layout, e `client.ts` anexa
  o token. Nunca guarde token em `localStorage` nem implemente login próprio.
- Datas enviadas com o fuso do navegador (`localInputToOffsetIso`).

## Convenções

- **Idioma:** código (identificadores) em inglês; mensagens ao usuário, issues, PRs e docs em português.
- **Branches:** `tipo/descricao-curta`: `feat/`, `fix/`, `refactor/`, `test/`, `security/`, `chore/`, `docs/`.
- **Commits:** Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`),
  referenciando a issue (`Closes #N`).
- **PRs:** um PR por fase/etapa do roadmap, descrevendo como foi testado e quais requisitos (IDs da spec) cobre. Mudança de comportamento atualiza a spec no mesmo PR.
- Correção de bug vem com teste que falharia antes da correção sempre que possível.
- Testes: unitários em `*Test` (surefire); os que exigem Docker em `*IT` (failsafe, `./mvnw verify`); marque cada teste com o requisito que cobre (`@req`).
  No frontend, `*.test.tsx` ao lado do código, usando `src/test/utils.tsx` (`renderRoute`, `mockFetch`);
  E2E em `e2e/`, simulando só a geocodificação.
- O CI (`.github/workflows/ci.yml`) e as varreduras (`security.yml`: gitleaks, Trivy, CodeQL; ZAP no
  E2E) precisam passar antes do merge.
- Java: Lombok só em entidades JPA; prefira `record` para DTOs e domínio.
- Python: tipagem com Pydantic `Field` para validar entradas.

## Segurança (regras não negociáveis)

- Nunca commitar segredos, `.env`, dumps de banco ou dados pessoais reais. Senhas e tokens não têm
  valor padrão no compose nem nos perfis; placeholders ficam só no `.env.example` (ver `docs/seguranca.md`).
- Dados de localização de motoristas/clientes são dados pessoais (LGPD): não logar coordenadas,
  endereços nem a busca de geocodificação (nem via mensagem de exceção com URL). Minimize a retenção.
  Mapa de dados em `docs/lgpd.md`.
- Artefatos de modelo `.pkl` executam código ao carregar: só carregue modelos gerados pelo próprio
  pipeline e com hash conferido (a issue #31 ainda prevê migrar para skops/ONNX).
- Containers rodam como não-root, com imagem de tag fixa, `read_only`, `cap_drop: ALL` e
  `no-new-privileges`. Serviços internos (ai-engine, banco) não publicam porta no host.
- Ações de agentes de IA que alteram dados exigem confirmação humana e respeitam o tenant do usuário.

## Estado conhecido / dívidas

- Perfis `DRIVER` e `CUSTOMER` ainda sem acesso: dependem do vínculo com a viagem (Fase 4, #38, #39).
- Rate limit em memória (por instância); multi-réplica exige Redis.
- LGPD: retenção e direitos do titular pendentes (#34).
- `is_rainy` sempre 0 e `confidence_score` fixo: issues #47 e #49.
- Distância em linha reta, não por estrada: issue #45.
- Dataset de treino é sintético (`generate_data.py`).
