# 0002 — Desenvolvimento orientado a especificação (SDD)

- **Status:** Aceita
- **Data:** 2026-11-27
- **Specs e issues:** `specs/`

## Contexto

O projeto vai crescer de um fluxo (viagem com ETA) para uma plataforma com tenants, frota, motoristas,
clientes, rastreamento e agentes de IA. Boa parte do código é escrita com apoio de agentes de IA, que
produzem mais e melhor quando partem de uma especificação explícita, e o mantenedor quer revisar as
ideias antes da implementação. Hoje o comportamento esperado só existe implícito nos testes e nas issues.

## Decisão

Adotamos Spec-Driven Development:

1. Cada funcionalidade tem `specs/NNN-nome/` com `spec.md` (o quê e por quê, requisitos com ID e
   critérios Dado/Quando/Então), `plan.md` (como) e `tasks.md` (passos).
2. A [constituição](../../specs/constitution.md) reúne os princípios que todo plano precisa respeitar.
3. O código só começa com a spec aprovada pelo mantenedor.
4. Os testes citam o requisito com `@req ID`, e o CI (`scripts/check_spec_traceability.py`) falha se
   uma spec implementada tiver requisito sem teste ou se um teste citar requisito inexistente.
5. Skills do Claude Code (`/spec-new`, `/spec-plan`, `/spec-implement`) e o subagente `spec-reviewer`
   automatizam o fluxo, sempre com revisão humana entre as etapas.

As funcionalidades já existentes ganham specs retroativas (001 a 003).

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Continuar só com issues | Issue descreve o pedido, não o comportamento vigente; fecha e envelhece |
| BDD com Cucumber executável | Exige uma camada de *step definitions* em três linguagens; o marcador `@req` dá rastreabilidade com custo quase zero |
| Ferramenta externa (Jira, Confluence) | Fica fora do repositório e do alcance dos agentes de IA |

## Consequências

- **Positivas:** revisão de ideias antes do código; requisitos rastreáveis até os testes; contexto
  estável para agentes de IA.
- **Negativas e custos:** um PR a mais (o da spec) por funcionalidade; specs precisam ser mantidas junto
  do código.
- **O que muda para quem desenvolve:** toda funcionalidade começa em `specs/`; testes novos levam
  `@req`.
