# Contratos entre serviços

Exemplos canônicos de requisição e resposta trocados entre os serviços. Os dois lados testam contra
estes mesmos arquivos, então uma mudança de campo de um lado só quebra o teste do outro:

| Arquivo | Quem produz | Quem consome | Testes |
|---|---|---|---|
| `ai-engine/predict-eta.request.json` | backend-core | ai-engine | `AiEngineContractTest` (Java) e `tests/test_contract.py` (Python) |
| `ai-engine/predict-eta.response.json` | ai-engine | backend-core | idem |

Os testes conferem os **nomes e tipos dos campos**, não os valores. Ao mudar o contrato, atualize o
exemplo e os dois serviços no mesmo PR.
