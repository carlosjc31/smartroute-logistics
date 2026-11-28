# Constituição do SmartRoute

Princípios que toda spec, plano e implementação precisam respeitar. Uma spec que contrarie um artigo
só avança se a constituição for emendada antes, por PR próprio aprovado pelo mantenedor e com ADR.
O [`CLAUDE.md`](../CLAUDE.md) detalha os comandos e convenções; este documento diz o que não se negocia.

## Artigo 1 — A spec vem antes do código

1. Toda funcionalidade nova ou mudança de comportamento começa por uma spec em `specs/NNN-nome/`
   (fluxo em [`specs/README.md`](README.md)). Correções de bug que restauram o comportamento já
   especificado não precisam de spec nova, mas o teste da correção cita o requisito violado.
2. A spec diz **o quê** e **por quê** (requisitos e critérios de aceite). O **como** fica no `plan.md`.
3. O código só começa depois que a spec está com `status: approved`, aprovada pelo mantenedor.
4. Todo requisito tem um ID estável (`PREFIXO-NNN`), nunca reaproveitado. Requisito removido fica
   marcado como `Removido`, com o motivo.
5. Todo requisito implementado é coberto por pelo menos um teste marcado com `@req PREFIXO-NNN`.
   O CI verifica (`scripts/check_spec_traceability.py`).

## Artigo 2 — Contrato antes da implementação

1. A API REST e o contrato com o ai-engine são definidos em `contracts/` antes do código que os
   implementa (contract-first, [ADR 0009](../docs/adr/0009-api-contract-first.md)).
2. Quebra de contrato exige nova versão do caminho (`/api/v2/...`) ou janela de compatibilidade
   descrita no plano. Os dois lados de um contrato mudam no mesmo PR.

## Artigo 3 — Arquitetura

1. Backend em **monólito modular**: um módulo por contexto de negócio, cada um hexagonal
   ([ADR 0003](../docs/adr/0003-arquitetura-hexagonal.md), [ADR 0004](../docs/adr/0004-monolito-modular.md)).
2. O domínio não depende de framework (Spring, JPA, Jackson).
3. Um módulo não acessa tabelas nem classes internas de outro: usa a API pública do módulo ou eventos.
4. Extrair um módulo para um serviço separado só com ADR que mostre qual critério de extração foi
   atingido (ADR 0004).
5. Schema muda só por migration Flyway nova. Migration mergeada nunca é editada.

## Artigo 4 — Segurança por padrão

1. Toda rota nova nasce negada (`denyAll`) e ganha regra explícita por perfil, com teste de acesso.
2. Nenhum segredo, `.env`, dump ou dado pessoal real no repositório. Senhas e tokens sem valor padrão.
3. Containers não-root, `read_only`, `cap_drop: ALL`, `no-new-privileges`; serviços internos sem porta
   publicada no host.
4. Aceitar uma CVE é decisão do mantenedor, registrada no `.trivyignore` com justificativa, issue e
   expiração ([ADR 0007](../docs/adr/0007-excecoes-de-cve-com-expiracao.md)).

## Artigo 5 — Privacidade (LGPD)

1. Toda spec que trate dado pessoal tem a seção **Dados pessoais**: quais dados, base legal,
   retenção e quem acessa. O [`docs/lgpd.md`](../docs/lgpd.md) é atualizado no mesmo PR.
2. Coordenadas, endereços e buscas de geocodificação nunca vão para log, mensagem de erro ou auditoria.
3. Retenção mínima: dado pessoal sem prazo de retenção definido não entra em produção.

## Artigo 6 — Rastreabilidade e auditoria

1. Toda alteração de dado de negócio gera registro de auditoria na mesma transação.
2. Dados de um tenant nunca são visíveis a outro (a partir da Fase 4, com Row-Level Security).

## Artigo 7 — Agentes de IA

1. Ação de agente que altera dados exige confirmação humana e respeita o tenant e o perfil do usuário.
2. Saídas de modelo são tratadas como entrada não confiável: validadas antes de usar.
3. Artefatos de modelo só são carregados se o hash conferir.

## Artigo 8 — Qualidade

1. Testes antes do merge: unitários, contrato e integração. Cobertura do backend ≥ 80% (JaCoCo).
2. O CI e as varreduras de segurança (`ci.yml`, `security.yml`) precisam passar.
3. Um PR por etapa do roadmap, descrevendo como foi testado e quais requisitos cobre.
4. Toda decisão de arquitetura que outra pessoa precisaria conhecer vira ADR em `docs/adr/`.
