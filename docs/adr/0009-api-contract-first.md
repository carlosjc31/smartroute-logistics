# 0009 — API contract-first com OpenAPI e geração de código

- **Status:** Aceita (implementada na R2, #91)
- **Data:** 2026-11-27 (implementação em 2026-11-28)
- **Specs e issues:** #17, #91, `contracts/api/openapi.yaml`

## Contexto

Antes da R2, o OpenAPI era gerado a partir do código (springdoc) e os tipos do frontend eram escritos à
mão em `src/api/`. O contrato só existia depois da implementação e podia divergir do frontend sem que
nenhum teste percebesse. Com SDD ([ADR 0002](0002-spec-driven-development.md)), o contrato deve ser
revisado junto do plano, antes do código.

## Decisão

- O contrato da API fica em `contracts/api/openapi.yaml`, escrito antes do código. Usa **OpenAPI
  3.0.3**: o suporte a 3.1 do openapi-generator ainda é parcial (nulos como `type: [x, "null"]`), e
  3.0.3 atende o que a API precisa.
- O backend gera as interfaces dos controllers (`com.logistics.logistics.api`) e os DTOs (`api.dto`, sufixo
  `DTO`) com o `openapi-generator-maven-plugin` (`interfaceOnly`), em `target/generated-sources`, sem
  versionar. Os controllers implementam as interfaces, e o mapper de cada módulo (`TripApiMapper`,
  `AuditApiMapper`) converte entre os DTOs e o domínio. O gerador produz classes, não records: é a
  exceção à preferência por records.
- Validações sem equivalente no OpenAPI entram como extensão do gerador no próprio contrato, por exemplo
  `x-field-extra-annotation: '@jakarta.validation.constraints.Future'`.
- `ApiContractTest` compara as rotas publicadas pelo backend com as operações do contrato: rota escrita
  à mão fora do contrato falha o build. `TripApiMapperTest` e `AuditApiMapperTest` conferem que os enums
  do contrato e do domínio têm os mesmos valores.
- O frontend gera `src/shared/api/schema.d.ts` com `openapi-typescript` (`npm run generate:api`), versionado;
  `src/shared/api/types.ts` só dá nomes aos tipos gerados. O CI falha se o arquivo gerado estiver desatualizado.
- O CI roda `oasdiff breaking` contra a branch de destino e falha em quebra de compatibilidade, a menos
  que o PR tenha o rótulo `quebra-de-contrato` (e o plano descreva a versão nova ou a janela de
  compatibilidade, constituição artigo 2).
- O springdoc continua servindo o Swagger UI fora de produção, agora a partir das anotações geradas do
  contrato.
- O contrato do ai-engine (`contracts/ai-engine/openapi.json`) é exportado do FastAPI pelos modelos
  Pydantic (`python -m app.contract`) e versionado (R4). `tests/test_contract.py` compara uma visão
  normalizada (rotas, campos, tipos, obrigatórios e limites) com o arquivo, para não quebrar com
  mudanças cosméticas de versão do FastAPI. O oasdiff checa as quebras como na API do backend. Os
  exemplos canônicos continuam verificados pelos dois lados.
- Eventos (WebSocket e outbox) são descritos em AsyncAPI em `contracts/events/` quando surgirem.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Manter code-first e só validar o spec gerado | Detecta divergência depois; o contrato não é revisado antes do código |
| Gerar só as interfaces e manter os DTOs records escritos à mão | Os DTOs poderiam divergir do contrato em nome, tipo ou validação sem erro de compilação |
| GraphQL ou gRPC | Troca de paradigma sem necessidade; REST + OpenAPI atende o painel e integrações |

## Consequências

- **Positivas:** contrato revisável no PR do plano; frontend e backend não divergem; quebras detectadas no CI.
- **Negativas e custos:** etapa de geração no build; DTOs gerados exigem mapeamento explícito para o
  domínio; o build Docker do backend precisa do contexto extra `contracts`.
- **O que muda para quem desenvolve:** mudar a API começa editando `openapi.yaml`; depois
  `./mvnw compile` (backend) e `npm run generate:api` (frontend).
