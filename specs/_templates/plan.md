# Plano técnico — NNN Nome da funcionalidade

Spec: [`spec.md`](spec.md) · Status da spec precisa ser `approved` antes deste plano virar código.

## Resumo da solução

Duas ou três frases sobre a abordagem.

## Verificação da constituição

| Artigo | Atende? | Observação |
|---|---|---|
| 1 — Spec antes do código | | |
| 2 — Contrato antes da implementação | | |
| 3 — Arquitetura (módulos, domínio sem framework) | | |
| 4 — Segurança por padrão | | |
| 5 — Privacidade (LGPD) | | |
| 6 — Rastreabilidade, auditoria e tenant | | |
| 7 — Agentes de IA | | Não se aplica, se for o caso |
| 8 — Qualidade | | |

Qualquer "não" precisa de justificativa e de emenda à constituição antes da implementação.

## Módulos afetados

| Módulo | Mudança | API pública nova ou alterada |
|---|---|---|

Comunicação entre módulos (chamada à API pública ou evento) e por quê.

## Modelo de domínio

Agregados, records, invariantes e máquina de estados, se houver.

## Contrato

- REST: trechos novos ou alterados em `contracts/api/openapi.yaml` (caminho, método, perfis, erros).
- Eventos: nome, payload e quem consome.
- Compatibilidade: quebra contrato? Se sim, como versionar.

## Persistência

Migrations Flyway novas (`V<n>__descricao.sql`), índices, RLS e impacto em dados existentes.

## Segurança

Regras novas no `SecurityConfig`, testes no `ApiSecurityTest`, segredos e superfícies de ataque novas.

## Observabilidade

Logs (sem dados pessoais), métricas e alertas.

## Estratégia de testes

| Requisito | Nível do teste | Onde |
|---|---|---|
| PREFIXO-001 | unitário / integração / contrato / E2E | |

## Riscos e alternativas descartadas

| Risco ou alternativa | Decisão |
|---|---|

## ADRs

- ADRs novas ou alteradas por este plano.
