# 0001 — Registrar decisões de arquitetura em ADRs

- **Status:** Aceita
- **Data:** 2026-11-27
- **Specs e issues:** —

## Contexto

As decisões das Fases 0 a 3 (hexagonal, Keycloak, exceções de CVE) ficaram espalhadas em descrições de
PR, no `CLAUDE.md` e em conversas. Quem chega ao projeto, inclusive agentes de IA, não tem como saber por
que algo é como é, e corre o risco de desfazer uma decisão sem perceber o que ela protegia. A Fase 4 vai
trazer várias decisões novas (multi-tenant, RLS, módulos, tempo real).

## Decisão

Registramos toda decisão de arquitetura relevante como ADR em `docs/adr/`, no formato do
[`template.md`](template.md). As decisões já tomadas nas Fases 0 a 3 entram como ADRs retroativas
(0003 a 0008).

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Só o `CLAUDE.md` | Diz o que fazer, mas não por quê nem quais alternativas foram descartadas; cresce sem limite |
| Wiki do GitHub | Fica fora do versionamento do código e do PR em que a decisão é tomada |

## Consequências

- **Positivas:** histórico das decisões revisado junto do código; agentes de IA leem as ADRs antes de
  propor mudanças estruturais.
- **Negativas e custos:** um arquivo a mais por decisão.
- **O que muda para quem desenvolve:** PR que muda arquitetura sem ADR não é aprovado.
