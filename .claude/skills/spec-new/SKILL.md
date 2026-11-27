---
name: spec-new
description: Cria a especificação (spec.md) de uma funcionalidade nova do SmartRoute em specs/NNN-nome/, no fluxo Spec-Driven Development do projeto, com requisitos com ID, critérios Dado/Quando/Então e seção LGPD. Use sempre que alguém quiser começar uma funcionalidade, uma etapa do roadmap ou uma issue (#36, "Fase 4.1", "multi-tenant", "cadastro de motoristas"), pedir para "especificar", "escrever a spec", "levantar requisitos", ou pedir para implementar algo que ainda não tem spec em specs/ — a spec vem antes do código pela constituição do projeto.
---

# Nova spec (SDD)

No SmartRoute, toda funcionalidade nasce de uma spec revisada pelo mantenedor antes do código
(constituição, artigo 1). A spec diz **o quê** e **por quê**; o **como** fica para o `plan.md`
(skill `spec-plan`). Uma boa spec evita que o código responda a perguntas que ninguém fez.

## 1. Reúna o contexto antes de perguntar

Leia, nesta ordem:

1. `specs/constitution.md`: os princípios que a spec não pode contrariar.
2. `specs/README.md`: o fluxo, os status e o índice das specs existentes.
3. `specs/_templates/spec.md`: o formato obrigatório.
4. A issue de origem, se houver: `gh issue view <N>`.
5. Specs relacionadas (as que a nova depende ou altera) e as ADRs em `docs/adr/` que tocam o assunto.
6. O código atual da área, só o suficiente para descrever o estado de hoje no **Contexto**.
7. `docs/lgpd.md`, se a funcionalidade tratar dados de pessoas (motoristas, clientes, localização).

Perguntar ao mantenedor o que está no repositório desperdiça a atenção dele.

## 2. Pergunte só o que for decisão do mantenedor

Junte as dúvidas em uma rodada (no máximo umas cinco perguntas, com uma recomendação em cada). São
perguntas típicas:

- objetivo de negócio e o que fica fora do escopo;
- quais perfis podem fazer o quê;
- prazo de retenção e base legal de dados pessoais;
- limites numéricos (tamanho, quantidade, tempo) que viram requisito não funcional.

O que não for respondido vira item em **Questões em aberto**, não um palpite escondido no requisito.

## 3. Escreva `specs/NNN-nome-curto/spec.md`

- **Número:** o maior `NNN` existente em `specs/` + 1. Nunca reaproveite um número.
- **Pasta:** `NNN-nome-curto`, em minúsculas e com hífens.
- **Prefixo:** curto, em maiúsculas e único entre as specs (confira no índice), por exemplo `TEN`,
  `FLEET`, `DRV`.
- **Cabeçalho:** `status: draft`, as issues, o módulo do backend dono (`module`) e `depends_on`.
- **Requisitos:** um comportamento observável por item, no formato `- **PREFIXO-NNN** — ...`. Use os
  padrões:
  - "Quando <evento>, o sistema deve...";
  - "Enquanto <estado>...";
  - "Se <condição indesejada>, então...".

  Numere em blocos (001+ funcionais, 010+ ou 020+ segurança e acesso, não funcionais depois), para que
  caiba requisito novo sem renumerar. Inclua sempre:
  - as regras de acesso por perfil das rotas novas (o padrão do projeto é negar);
  - a auditoria das alterações de dados;
  - a separação por tenant, quando houver dado de transportadora.
- **Critérios de aceite:** pelo menos um cenário Dado/Quando/Então para cada requisito funcional
  importante, citando o ID no título do cenário. Use valores concretos ("4,92 h → 12:55"), não
  "um valor válido".
- **Dados pessoais (LGPD):** obrigatória quando a funcionalidade trata dado pessoal. Uma linha por dado,
  com titular, base legal, retenção e quem acessa. Se a funcionalidade não trata dado pessoal, escreva
  isso explicitamente.

Critérios para cada requisito (revise um por um antes de terminar):

- dá para escrever um teste que passa ou falha sem interpretação;
- diz o comportamento, não a implementação ("deve recusar com 409", não "o service lança exceção");
- não tem "rápido", "amigável" ou "robusto" sem número;
- não duplica um requisito de outra spec: referencie a outra spec em vez disso. A spec 004 (`SEC`)
  cobre autenticação, CORS, headers, rate limit e formato de erro de todas as rotas.

## 4. Feche

1. Adicione a linha da spec no índice de `specs/README.md`.
2. Rode `python3 scripts/check_spec_traceability.py`. Uma spec `draft` não exige testes, mas o
   formato, os IDs e o prefixo precisam passar.
3. Não escreva `plan.md`, `tasks.md` nem código, e não mude o status para `approved`: só o mantenedor
   aprova. Sugira um PR só com a spec, na branch `docs/spec-NNN-nome-curto`, e liste as questões em
   aberto que bloqueiam a aprovação.
