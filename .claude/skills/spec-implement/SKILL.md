---
name: spec-implement
description: Implementa uma spec aprovada do SmartRoute seguindo o tasks.md em specs/NNN-nome/, task por task, com teste primeiro, marcadores @req nos testes e as regras da constituição (rota negada por padrão, auditoria, migrations Flyway, LGPD). Use quando alguém pedir para "implementar a spec", "executar as tasks", "começar o código da etapa", "seguir o plano" ou "continuar a implementação" de uma funcionalidade que já tem spec aprovada, plan.md e tasks.md, inclusive para retomar uma implementação pela metade.
---

# Implementar uma spec (SDD)

A implementação segue o plano aprovado; ela não o redesenha. O valor do SDD está em o código corresponder
ao que foi revisado. Quando a realidade contradiz a spec ou o plano, a resposta é atualizar o documento
com o mantenedor, não divergir em silêncio.

## 1. Pré-condições

- `spec.md` com `status: approved` (ou `implemented`, para uma mudança posterior), e com `plan.md` e
  `tasks.md` na mesma pasta. Se faltar algum, pare e indique a skill `spec-new` ou `spec-plan`.
- Uma branch própria a partir da `main` (`feat/`, `fix/`, `refactor/`... conforme o `CLAUDE.md`).
  Não trabalhe direto na `main`.
- Leia `specs/constitution.md`, a spec, o plano e as tasks, e as seções "Arquitetura do backend",
  "Frontend" e "Segurança" do `CLAUDE.md`.

Retomando uma implementação? As tasks marcadas `[x]` estão feitas. Confirme com `git log` e com os
testes, e continue da primeira task aberta.

## 2. Para cada task, em ordem

1. **Teste primeiro.** Escreva o teste que expressa o requisito citado na task, com o marcador em
   comentário logo acima:
   - Java: `// @req TEN-001`
   - TypeScript: `// @req TEN-001`
   - Python: `# @req TEN-001`

   Rode o teste e veja-o falhar pelo motivo certo.
2. **Implemente o mínimo** para o teste passar, respeitando a arquitetura:
   - o domínio não importa Spring, JPA nem Jackson;
   - a aplicação não importa infraestrutura;
   - um módulo não usa classes internas nem tabelas de outro;
   - a API expõe DTOs.
3. **Regras que não se negociam,** sempre que a task tocar no assunto:
   - **Rota nova:** regra explícita no `SecurityConfig` e um caso no `ApiSecurityTest` que mostra tanto
     o acesso permitido quanto o negado.
   - **Alteração de dado de negócio:** registro pelo `AuditService` na mesma transação, sem coordenadas
     nem endereços.
   - **Schema:** só por migration nova `V<n>__descricao.sql`.
   - **Dados pessoais:** nunca em log nem em mensagem de exceção.
   - **Datas e horário:** `OffsetDateTime` e `Clock` injetado.
   - **Erros:** saem em RFC 7807.
4. **Rode os testes da área**, com os comandos do `CLAUDE.md`:
   - backend: `./mvnw test -Dtest=...`;
   - ai-engine: `pytest`;
   - frontend: `npm test`.
5. **Marque a task como `[x]`** no `tasks.md`.

Se surgir algo que a spec não cobre ou contradiz (um caso de erro sem resposta definida, um requisito
impossível como escrito, uma decisão que o plano não tomou), pare naquela task. Explique o conflito,
proponha a alteração na spec ou no plano e espere o mantenedor, porque mudar requisito é decisão dele.
Ajustes que não mudam comportamento (nome de classe, organização interna) podem seguir, anotados no
plano.

## 3. Fechamento

1. Rode a verificação completa e corrija o que falhar:
   - `cd backend-core && ./mvnw verify`, com os `*IT` e o JaCoCo ≥ 80%, se houver Docker;
   - `cd ai-engine && pytest`, se o ai-engine mudou;
   - `cd smartroute-frontend && npm test && npm run lint && npm run build`, se o frontend mudou;
   - `python3 scripts/check_spec_traceability.py`.
2. Atualize a documentação que a funcionalidade afeta:
   - `docs/lgpd.md` (dados pessoais novos);
   - `docs/seguranca.md` (perfis, rotas);
   - `CLAUDE.md` (comandos, variáveis, estado conhecido);
   - o índice de `specs/README.md`.
3. Só mude a spec para `status: implemented` quando todos os requisitos não pendentes tiverem teste e o
   verificador passar.
4. Não faça commit nem push sem o pedido do mantenedor. Ao pedir, use Conventional Commits em
   português, citando a issue (`Closes #N`). Prepare a descrição do PR com:
   - os requisitos cobertos;
   - como foi testado;
   - o que ficou pendente.
