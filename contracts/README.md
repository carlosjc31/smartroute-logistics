# Contratos entre serviços

Os contratos vêm antes do código que os implementa (constituição, artigo 2;
[ADR 0009](../docs/adr/0009-api-contract-first.md)).

## API do backend-core: `api/openapi.yaml`

OpenAPI 3.0.3 com todas as rotas de `/api/v1`, os erros RFC 7807 e o perfil exigido por operação.

| Quem usa | Como | Verificação |
|---|---|---|
| backend-core | `openapi-generator-maven-plugin` gera as interfaces (`controller/api`) e os DTOs (`controller/dto`) no build | `ApiContractTest` (nenhuma rota fora do contrato) e compilação dos controllers |
| frontend | `npm run generate:api` gera `src/api/schema.d.ts` | CI falha se o arquivo gerado estiver desatualizado |
| CI | `oasdiff breaking` contra a branch de destino | Falha em quebra de compatibilidade sem o rótulo `quebra-de-contrato` |

Para mudar a API:

1. Edite `api/openapi.yaml` (no PR do plano da spec, quando houver).
2. Backend: `./mvnw compile` regera o código; ajuste o controller e o `ApiMapper`.
3. Frontend: `npm run generate:api` e versione `src/api/schema.d.ts`.
4. Quebra de compatibilidade (campo removido, validação mais restrita, rota removida) exige `/api/v2` ou
   uma janela de compatibilidade descrita no plano, e o rótulo `quebra-de-contrato` no PR.

## ai-engine: exemplos canônicos

Exemplos de requisição e resposta trocados entre o backend e o ai-engine. Os dois lados testam contra
estes mesmos arquivos, então uma mudança de campo de um lado só quebra o teste do outro:

| Arquivo | Quem produz | Quem consome | Testes |
|---|---|---|---|
| `ai-engine/predict-eta.request.json` | backend-core | ai-engine | `AiEngineContractTest` (Java) e `tests/test_contract.py` (Python) |
| `ai-engine/predict-eta.response.json` | ai-engine | backend-core | idem |

Os testes conferem os **nomes e tipos dos campos**, não os valores. Ao mudar o contrato, atualize o
exemplo e os dois serviços no mesmo PR. Este contrato vira OpenAPI na R4.
