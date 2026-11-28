---
id: "003"
title: Inferência de ETA (ai-engine)
prefix: ETA
status: implemented
issues: [9, 23, 24, 31]
module: ai-engine
depends_on: []
---

# 003 — Inferência de ETA (ai-engine)

> Spec retroativa: descreve o comportamento entregue nas Fases 0 a 3.

## Contexto

O backend precisa de uma estimativa da duração da viagem que considere distância, carga e horário de pico.
O modelo (`RandomForestRegressor`) é treinado em Python e servido pelo ai-engine
([ADR 0006](../../docs/adr/0006-ai-engine-servico-separado.md)). O dataset ainda é sintético.

## Objetivos

- Prever a duração em horas decimais com erro médio de no máximo 30 minutos.
- Ser acessível só pelo backend, e nunca executar um artefato de modelo adulterado.

## Fora do escopo

- Clima real (#47), intervalo de confiança real (#49), re-treino com dados reais (#48), drift (#50).

## Atores e perfis

| Ator | O que pode fazer |
|---|---|
| backend-core (token de serviço) | Pedir previsões |
| Orquestrador (Docker, Kubernetes) | Consultar `/health` |

## Requisitos

### Funcionais

- **ETA-001** — Quando o backend envia `distance`, `weight`, `hour_of_day` e `is_rainy` em
  `POST /predict/eta`, o serviço deve devolver `predicted_arrival_hours` positivo e decimal,
  `confidence_score`, `model_version`, `status` e `metadata.traffic_impact`, no formato de
  `contracts/ai-engine/`.
- **ETA-002** — Nos horários de pico (7–9h e 17–19h), a previsão deve ser maior que fora do pico para a
  mesma viagem, e `traffic_impact` deve ser `High`.
- **ETA-003** — Se a entrada for inválida ou incompleta, o serviço deve responder `422`.
- **ETA-004** — Se o modelo não estiver carregado, o serviço deve subir mesmo assim, informar isso no
  `/health` e responder `503` às previsões.
- **ETA-005** — O contrato deve ser compatível nos dois lados: o que o backend envia é aceito, e tudo o
  que o serviço devolve é lido pelo backend, sem campo desconhecido.

### Segurança

- **ETA-010** — Previsões devem exigir `Authorization: Bearer <SERVICE_TOKEN>`, comparado em tempo
  constante; sem token ou com token errado, `401`. O `/health` é público. Sem `SERVICE_TOKEN`, o serviço
  não sobe.
- **ETA-011** — O modelo só deve ser carregado se o SHA-256 do arquivo bater com o hash gerado pelo
  treino ou com `MODEL_SHA256`; caso contrário, fica descarregado (ETA-004).
- **ETA-012** — Erros internos não devem expor detalhes (mensagem ou stack trace) na resposta.
- **ETA-013** [pendente] — O modelo deve usar um formato que não executa código ao carregar (skops ou
  ONNX, #31).

### Não funcionais

- **ETA-020** — O erro médio absoluto do modelo versionado deve ser menor que 30 minutos em viagens que
  ele não viu no treino.

## Critérios de aceite

```gherkin
Cenário: previsão no horário de pico (ETA-002)
  Dado uma viagem de 360 km com 10 t
  Quando o backend pede a previsão para as 8h e para as 13h
  Então a previsão das 8h é maior e tem traffic_impact "High"

Cenário: modelo adulterado (ETA-011)
  Dado um arquivo de modelo cujo SHA-256 não confere
  Quando o serviço inicia
  Então o modelo não é carregado e /predict/eta responde 503
```

## Dados pessoais (LGPD)

Não trata dados pessoais: recebe só distância, peso, hora e chuva, sem coordenadas.

## Questões em aberto

- [ ] Limite de erro aceitável quando houver dados reais (#48).
