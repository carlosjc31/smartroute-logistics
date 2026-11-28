---
id: "NNN"
title: Nome da funcionalidade
prefix: PREFIXO            # único entre as specs, em maiúsculas (ex.: TEN, FLEET)
status: draft              # draft | approved | implemented | superseded
issues: []                 # ex.: [36]
module: nome-do-modulo     # módulo do backend que é dono da funcionalidade
depends_on: []             # ex.: ["001"]
---

# NNN — Nome da funcionalidade

## Contexto

Qual problema de quem estamos resolvendo, e por que agora. Uma ou duas frases sobre o estado atual.

## Objetivos

- O que precisa ser verdade quando terminar.

## Fora do escopo

- O que conscientemente fica de fora (e, se souber, em qual spec ou issue entra).

## Atores e perfis

| Perfil | O que pode fazer |
|---|---|
| `ADMIN` | |
| `DISPATCHER` | |

## Requisitos

Cada requisito é verificável, tem um ID estável e diz o comportamento observável, não a implementação.
Use "deve" para obrigatório. Marque `[pendente]` o que fica para depois e `[removido]` o que saiu
(mantendo o ID).

### Funcionais

- **PREFIXO-001** — Quando <evento>, o sistema deve <resposta>.
- **PREFIXO-002** — Enquanto <estado>, o sistema deve <comportamento>.
- **PREFIXO-003** — Se <condição indesejada>, então o sistema deve <reação>.

### Segurança e acesso

- **PREFIXO-0XX** — Somente <perfis> devem poder <ação>; os demais recebem `403`.

### Não funcionais

- **PREFIXO-0XX** — <desempenho, disponibilidade, limites, observabilidade>.

## Critérios de aceite

Cenários concretos, um ou mais por requisito, no formato Dado/Quando/Então.

```gherkin
Cenário: <nome> (PREFIXO-001)
  Dado <contexto>
  Quando <ação>
  Então <resultado observável>
```

## Dados pessoais (LGPD)

Obrigatória quando a funcionalidade trata dado pessoal (constituição, artigo 5). Se não trata, escreva
"Não trata dados pessoais".

| Dado | Titular | Base legal | Retenção | Quem acessa |
|---|---|---|---|---|

## Questões em aberto

- [ ] Pergunta que precisa de resposta antes da aprovação (e de quem).
