---
id: "001"
title: Viagens e previsão de chegada
prefix: TRIP
status: implemented
issues: [3, 6, 7, 8, 13, 14, 15, 16, 28, 35]
module: trip
depends_on: ["002", "003", "004"]
---

# 001 — Viagens e previsão de chegada

> Spec retroativa: descreve o comportamento entregue nas Fases 0 a 3.

## Contexto

A pessoa responsável pelo despacho precisa registrar uma viagem (origem, destino, carga e saída) e saber
quando ela deve chegar, para planejar a operação e avisar o cliente. A previsão vem do modelo de ML do
ai-engine ([spec 003](../003-eta-inference/spec.md)); se ele estiver fora, o despacho não pode parar.

## Objetivos

- Registrar viagens com chegada prevista em minutos, mesmo com o ai-engine indisponível.
- Acompanhar o ciclo de vida da viagem com transições validadas.
- Guardar quem mudou o quê e quando, sem expor dados de localização.

## Fora do escopo

- Viagens com várias paradas, alocação de veículo e motorista (#40).
- Distância por estrada (#45); hoje é em linha reta (Haversine).
- Acesso de motoristas e clientes às viagens (#38, #39).
- Separação por transportadora (multi-tenant, #36).

## Atores e perfis

| Perfil | O que pode fazer |
|---|---|
| `DISPATCHER` | Criar, listar, detalhar e mudar o status de viagens |
| `ADMIN` | O mesmo que `DISPATCHER`, e ler a auditoria |
| `DRIVER`, `CUSTOMER` | Nada ainda |

## Requisitos

### Funcionais

- **TRIP-001** — Quando o despachante envia origem e destino (coordenadas), rótulos opcionais (até 200
  caracteres), peso da carga em toneladas e horário de saída futuro com fuso, o sistema deve criar a
  viagem com status `PENDING` e responder `201` com o cabeçalho `Location`.
- **TRIP-002** — Se os dados forem inválidos (campo ausente, latitude fora de ±90, longitude fora de
  ±180, peso não positivo, saída no passado, origem igual ao destino), o sistema deve recusar com `400`
  no formato RFC 7807 e com os erros por campo, quando houver.
- **TRIP-003** — Ao criar a viagem, o sistema deve calcular a distância em linha reta (Haversine), pedir
  a previsão ao ai-engine com distância, peso, hora local da saída e chuva, e gravar a chegada prevista
  como saída + duração prevista arredondada ao minuto, no fuso da saída, junto da versão do modelo e da
  origem `MODEL`.
- **TRIP-004** — Se o ai-engine estiver indisponível (erro, timeout ou circuito aberto, depois das
  novas tentativas), o sistema deve criar a viagem mesmo assim, com a estimativa heurística (80 km/h,
  atraso proporcional ao peso, ×1,5 nos picos 7–9h e 17–19h) e origem `FALLBACK`, sinalizada no painel.
- **TRIP-005** — O sistema deve listar as viagens da mais recente para a mais antiga, paginadas (página
  padrão 0, tamanho padrão 20, máximo 100), com filtro opcional por status. Tamanho acima do máximo
  deve ser recusado com `400`.
- **TRIP-006** — O sistema deve detalhar uma viagem pelo id, com a chegada prevista e o modelo usado.
  Id desconhecido deve responder `404`.
- **TRIP-007** — O sistema deve aceitar só estas transições de status: `PENDING → IN_TRANSIT |
  CANCELLED`, `IN_TRANSIT → DELAYED | COMPLETED`, `DELAYED → COMPLETED`. `COMPLETED` e `CANCELLED` são
  finais. Transição inválida deve responder `409`, e o painel deve oferecer só as transições permitidas.
- **TRIP-008** — Quando a viagem é concluída, o sistema deve registrar a chegada real. As demais
  transições preservam a previsão e não alteram a chegada real.
- **TRIP-009** — No painel, o despachante deve localizar origem e destino pelo nome
  ([spec 002](../002-geocoding/spec.md)), criar a viagem e ser levado ao detalhe dela. A saída sugerida
  é a próxima hora cheia e é enviada com o fuso do navegador.

### Auditoria

- **TRIP-010** — A criação e cada mudança de status devem gerar um registro de auditoria na mesma
  transação, com ator (`preferred_username`), data/hora, ação e estados antes/depois. Transição recusada
  não gera registro.
- **TRIP-011** — Os estados auditados nunca devem conter coordenadas nem rótulos de endereço.
- **TRIP-012** — O banco deve recusar `UPDATE`, `DELETE` e `TRUNCATE` na tabela de auditoria.
- **TRIP-013** — O histórico de auditoria de uma viagem deve ser lido em
  `GET /api/v1/audit/trips/{id}`, do mais recente para o mais antigo.

### Eventos

- **TRIP-014** — Quando uma viagem é criada ou muda de status, o sistema deve publicar o evento
  `TripCreated` ou `TripStatusChanged` na mesma transação, com o id da viagem, o status (anterior e novo,
  na mudança) e as datas, e nunca com coordenadas nem rótulos de endereço. Transição recusada não publica
  evento. Os consumidores (notificações, #43) recebem depois do commit, pelo outbox (ADR 0004).

### Segurança e acesso

- **TRIP-020** — Somente `ADMIN` e `DISPATCHER` devem acessar `/api/v1/trips/**`; os demais perfis
  recebem `403`.
- **TRIP-021** — Somente `ADMIN` deve ler a auditoria; os demais perfis recebem `403`.
- **TRIP-022** [pendente] — `DRIVER` deve ver só as viagens em que está alocado (#38, Fase 4).
- **TRIP-023** [pendente] — `CUSTOMER` deve ver só as viagens dos próprios pedidos (#39, Fase 4).

## Critérios de aceite

```gherkin
Cenário: previsão do modelo preserva os minutos (TRIP-003)
  Dado que o ai-engine prevê 4,92 horas
  Quando o despachante cria uma viagem com saída em 2030-01-01T08:00-03:00
  Então a chegada prevista é 2030-01-01T12:55-03:00 e a origem da previsão é MODEL

Cenário: ai-engine fora do ar (TRIP-004)
  Dado que o ai-engine responde 503 em todas as tentativas
  Quando o despachante cria uma viagem
  Então a resposta é 201, a origem da previsão é FALLBACK e o painel avisa que a previsão é estimada

Cenário: transição inválida (TRIP-007)
  Dado uma viagem COMPLETED
  Quando alguém pede a transição para IN_TRANSIT
  Então a resposta é 409 em RFC 7807 e nenhum registro de auditoria é criado

Cenário: auditoria sem localização (TRIP-010, TRIP-011)
  Dado uma viagem de São Paulo para o Rio de Janeiro
  Quando ela é criada e depois colocada em trânsito
  Então há dois registros de auditoria com o ator, e nenhum contém coordenadas ou rótulos

Cenário: motorista ainda sem acesso (TRIP-020)
  Dado um usuário com o perfil DRIVER
  Quando ele lista as viagens
  Então a resposta é 403 "Acesso negado"
```

## Dados pessoais (LGPD)

| Dado | Titular | Base legal | Retenção | Quem acessa |
|---|---|---|---|---|
| Coordenadas e rótulos de origem/destino | Cliente ou destinatário, quando é endereço de pessoa física | Execução de contrato | Indefinida hoje (#34) | `ADMIN`, `DISPATCHER` |
| Ator da auditoria | Funcionário | Legítimo interesse | Indefinida hoje (#34) | `ADMIN` |

Mapa completo em [`docs/lgpd.md`](../../docs/lgpd.md).

## Questões em aberto

- [ ] Prazo de retenção das viagens e da auditoria (#34).
