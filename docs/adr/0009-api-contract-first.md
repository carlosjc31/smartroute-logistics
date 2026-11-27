# 0009 — API contract-first com OpenAPI e geração de código

- **Status:** Aceita (implementação na etapa R2)
- **Data:** 2026-11-27
- **Specs e issues:** #17, `contracts/`

## Contexto

Hoje o OpenAPI é gerado a partir do código (springdoc) e os tipos do frontend são escritos à mão em
`src/api/`. O contrato só existe depois da implementação e pode divergir do frontend sem que nenhum teste
perceba. Com SDD ([ADR 0002](0002-spec-driven-development.md)), o contrato deveria ser revisado junto do
plano, antes do código.

## Decisão

- O contrato da API fica em `contracts/api/openapi.yaml` (OpenAPI 3.1), escrito antes do código.
- O backend gera interfaces de controller e DTOs (records) com o `openapi-generator-maven-plugin`
  (`interfaceOnly`); os controllers implementam as interfaces.
- O frontend gera os tipos com `openapi-typescript`, no lugar dos tipos escritos à mão.
- O CI roda `oasdiff` contra a `main` e falha em quebra de contrato não sinalizada.
- O contrato do ai-engine também vira OpenAPI em `contracts/ai-engine/`, verificado contra o que o
  FastAPI gera.
- Eventos (WebSocket e outbox) são descritos em AsyncAPI em `contracts/events/` quando surgirem.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Manter code-first e só validar o spec gerado | Detecta divergência depois; o contrato não é revisado antes do código |
| GraphQL ou gRPC | Troca de paradigma sem necessidade; REST + OpenAPI atende o painel e integrações |

## Consequências

- **Positivas:** contrato revisável no PR do plano; frontend e backend não divergem; quebras detectadas no CI.
- **Negativas e custos:** etapa de geração no build; DTOs gerados exigem mapeamento explícito para o domínio.
- **O que muda para quem desenvolve:** mudar a API começa editando `openapi.yaml`.
