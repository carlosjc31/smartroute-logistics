---
name: spec-plan
description: Gera o plano técnico (plan.md) e as tarefas (tasks.md) de uma spec aprovada do SmartRoute em specs/NNN-nome/, verificando a constituição, o contrato da API, as migrations, a segurança e a estratégia de testes por requisito. Use quando uma spec estiver com status approved e alguém pedir para "planejar", "desenhar a solução", "quebrar em tarefas", "fazer o plano técnico" ou "preparar a implementação" de uma spec ou etapa do roadmap, e também antes de começar o código de uma funcionalidade que tem spec mas ainda não tem plan.md.
---

# Plano e tarefas de uma spec aprovada (SDD)

O plano transforma o **o quê** da spec em um **como** revisável, e as tasks quebram esse como em passos
pequenos que alguém (pessoa ou agente) executa sem precisar redescobrir o desenho. Revisar um plano é
muito mais barato que revisar um PR grande cuja abordagem já nasceu errada.

## 1. Confirme a pré-condição

Abra `specs/NNN-*/spec.md`. Se o cabeçalho não estiver com `status: approved`, pare e diga ao
mantenedor que a spec precisa ser aprovada antes. Se houver questões em aberto que mudam o desenho,
liste-as em vez de planejar sobre um palpite.

## 2. Leia

- A spec inteira e as specs de `depends_on`.
- `specs/constitution.md` e as ADRs em `docs/adr/` (todas as aceitas; são poucas e curtas).
- `CLAUDE.md`, em especial "Arquitetura do backend" e "Segurança".
- O código dos módulos afetados: domínio, portas, adapters, `SecurityConfig`, migrations existentes
  (para saber o próximo `V<n>`) e o contrato atual em `contracts/`.

## 3. Escreva `plan.md` a partir de `specs/_templates/plan.md`

Pontos que costumam ser esquecidos e custam caro depois:

- **Verificação da constituição:** preencha cada artigo com "sim" e como, ou "não se aplica" e por
  quê. Um "não" bloqueia: proponha a emenda (com ADR) em vez de contornar.
- **Módulos:** o backend é um monólito modular (ADR 0004). Diga qual módulo é dono de cada dado, qual
  API pública ou evento um módulo usa para falar com outro, e nunca planeje ler tabela de outro
  módulo. Se um requisito parecer exigir um serviço separado, mostre qual critério de extração da
  ADR 0004 foi atingido.
- **Contrato primeiro (ADR 0009):** descreva as rotas novas ou alteradas como trecho de OpenAPI
  (caminho, método, corpo, respostas de erro, perfis). Se `contracts/api/openapi.yaml` já existir,
  a primeira task altera esse arquivo. Diga se a mudança quebra o contrato e, se quebrar, como
  versionar.
- **Persistência:**
  - migrations novas, sem editar migration mergeada;
  - índices;
  - coluna `tenant_id` e política de RLS quando o dado for de uma transportadora;
  - o que acontece com os dados que já existem.
- **Segurança e privacidade:**
  - regras novas no `SecurityConfig` e os casos no `ApiSecurityTest`;
  - auditoria pelo `AuditService` na mesma transação;
  - o que nunca pode ir para log;
  - a retenção dos dados pessoais.
- **Estratégia de testes:** uma linha por requisito da spec, com o nível do teste (unitário, contrato,
  integração `*IT`, Vitest, E2E). Todo requisito não pendente precisa aparecer.
- **ADRs:** decisão de arquitetura nova (biblioteca relevante, padrão de comunicação, trade-off que
  alguém poderia querer desfazer) vira `docs/adr/NNNN-titulo.md`, a partir de `docs/adr/template.md`,
  e entra no índice `docs/adr/README.md`.

## 4. Escreva `tasks.md` a partir de `specs/_templates/tasks.md`

- Ordem: contrato → domínio → aplicação → persistência → API e segurança → frontend → docs e fechamento.
- Cada task cabe em um commit, termina com um teste passando e cita os requisitos que cobre, por
  exemplo `(TEN-001, TEN-002)`.
- Marque `[P]` só quando a task não toca os mesmos arquivos da anterior.
- As duas últimas tasks são sempre: atualizar a documentação (`docs/lgpd.md`, `docs/seguranca.md`,
  `CLAUDE.md`, o índice de specs) e rodar o verificador de rastreabilidade, mudando a spec para
  `implemented`.

Antes de terminar, confira que cada ID de requisito não pendente aparece em pelo menos uma task. Um
requisito sem task é um requisito que não vai ser feito.

## 5. Feche

Não escreva código de produção nesta etapa. Resuma para o mantenedor:

- a abordagem;
- as decisões que pedem a opinião dele, cada uma com uma recomendação;
- as ADRs novas;
- o número de tasks.

Sugira revisar o plano no mesmo PR da spec ou em um PR próprio antes da implementação, que usa a skill
`spec-implement`.
