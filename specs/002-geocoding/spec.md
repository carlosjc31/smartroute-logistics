---
id: "002"
title: Geocodificação
prefix: GEO
status: implemented
issues: [20]
module: geocoding
depends_on: ["004"]
---

# 002 — Geocodificação

> Spec retroativa: descreve o comportamento entregue nas Fases 1 a 3.

## Contexto

O despachante conhece as cidades pelo nome, não pelas coordenadas. O frontend chamava o Nominatim direto,
expondo a busca de cada usuário a um terceiro e violando a política de uso do provedor (máximo de 1 req/s,
User-Agent identificável).

## Objetivos

- Converter um nome de lugar em coordenadas pelo backend, com cache.
- Respeitar a política do Nominatim e não vazar a busca em logs.

## Fora do escopo

- Autocompletar enquanto digita.
- Geocodificação reversa (coordenada → endereço).
- Provedor próprio ou pago (avaliar com #45).

## Atores e perfis

| Perfil | O que pode fazer |
|---|---|
| `ADMIN`, `DISPATCHER` | Geocodificar |
| `DRIVER`, `CUSTOMER` | Nada |

## Requisitos

### Funcionais

- **GEO-001** — Quando o usuário busca um lugar em `GET /api/v1/geocode?q=`, o sistema deve devolver as
  coordenadas e o nome completo do primeiro resultado, com a busca sem espaços nas pontas e restrita aos
  países de `GEOCODING_COUNTRY_CODES` (sem restrição se vazio).
- **GEO-002** — Se nenhum lugar for encontrado, o sistema deve responder `404`, e o painel deve avisar
  qual cidade não foi encontrada.
- **GEO-003** — Resultados encontrados devem ficar em cache por 24 horas, sem diferenciar maiúsculas e
  espaços nas pontas. Buscas sem resultado não entram no cache.

### Integração com o provedor

- **GEO-004** — O sistema deve fazer no máximo uma requisição ao Nominatim por intervalo configurado
  (1 s por padrão), com o User-Agent configurado.
- **GEO-005** — Se o provedor falhar, o sistema deve responder `503` sem incluir a busca na mensagem de
  erro nem nos logs.

### Segurança e acesso

- **GEO-010** — Somente `ADMIN` e `DISPATCHER` devem geocodificar; os demais perfis recebem `403`.

## Critérios de aceite

```gherkin
Cenário: cidade encontrada (GEO-001)
  Dado que o Nominatim conhece "Campinas" no Brasil
  Quando o despachante busca "  Campinas "
  Então o provedor recebe q=Campinas, limit=1 e countrycodes=br
  E a resposta traz -22.9056, -47.0608 e "Campinas, São Paulo, Brasil"

Cenário: segunda busca usa o cache (GEO-003)
  Dado que "Campinas" já foi buscada
  Quando alguém busca "campinas"
  Então o provedor não é chamado de novo

Cenário: provedor fora do ar (GEO-005)
  Dado que o Nominatim responde 500
  Quando o despachante busca "Rua das Flores, 10"
  Então a resposta é 503 e o log não contém "Rua das Flores"
```

## Dados pessoais (LGPD)

| Dado | Titular | Base legal | Retenção | Quem acessa |
|---|---|---|---|---|
| Texto da busca (pode ser endereço) | Cliente ou destinatário | Execução de contrato | 24 h em memória; enviado ao Nominatim | Ninguém (não é persistido nem logado) |

## Questões em aberto

- [ ] Avaliar provedor com contrato de tratamento de dados antes de produção (o Nominatim público não tem).
