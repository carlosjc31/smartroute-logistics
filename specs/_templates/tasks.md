# Tasks — NNN Nome da funcionalidade

Spec: [`spec.md`](spec.md) · Plano: [`plan.md`](plan.md)

Regras:
- Cada task é pequena (cabe em um commit), tem um resultado verificável e cita os requisitos que cobre.
- Ordem: contrato → domínio → aplicação → persistência → adapters/API → frontend → docs.
- Teste primeiro sempre que possível: a task só termina com o teste passando.
- `[P]` marca tasks que podem rodar em paralelo com a anterior (não tocam os mesmos arquivos).

## Contrato

- [ ] T001 — Descrever <rota> em `contracts/api/openapi.yaml` (PREFIXO-001)

## Domínio

- [ ] T002 — <record/regra> com invariantes e testes unitários (PREFIXO-001, PREFIXO-002)

## Aplicação

- [ ] T003 — Caso de uso <nome> com auditoria na mesma transação (PREFIXO-001)

## Persistência

- [ ] T004 — Migration `V<n>__<descricao>.sql` e adapter JPA com teste no PostgreSQL embarcado

## API e segurança

- [ ] T005 — Controller, regra no `SecurityConfig` e casos no `ApiSecurityTest` (PREFIXO-0XX)

## Frontend

- [ ] T006 [P] — Página/componente <nome> com testes Vitest (PREFIXO-001)

## Fechamento

- [ ] T007 — Atualizar `docs/` (`lgpd.md`, `seguranca.md`), `CLAUDE.md` e o índice de specs
- [ ] T008 — `python3 scripts/check_spec_traceability.py` sem falhas e spec em `status: implemented`
