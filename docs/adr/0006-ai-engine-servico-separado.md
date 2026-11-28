# 0006 — Inferência de ETA em serviço Python separado

- **Status:** Aceita (registro retroativo)
- **Data:** 2026-11-27
- **Specs e issues:** #9, #24, #31, `specs/003-eta-inference`

## Contexto

O modelo de ETA é treinado com scikit-learn, e o ecossistema de ML (treino, MLflow, drift) é Python.
Servir o modelo dentro da JVM exigiria converter o modelo ou manter duas stacks no mesmo processo.

## Decisão

- O ai-engine é um serviço FastAPI separado, só acessível pela rede interna, com token de serviço
  comparado em tempo constante.
- O contrato `POST /predict/eta` fica em `contracts/ai-engine/` e é testado pelos dois lados.
- O modelo só carrega se o SHA-256 conferir; se o serviço falhar, o backend usa a estimativa heurística
  do domínio e sinaliza `eta.source = FALLBACK`.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Modelo na JVM (ONNX Runtime Java, PMML) | Afasta o treino da inferência; o time de ML trabalharia em duas stacks |
| Chamar um serviço de ML gerenciado | Custo e dados de rota saindo da infraestrutura |

## Consequências

- **Positivas:** ciclo de vida do modelo independente; falha do ai-engine não derruba o despacho.
- **Negativas e custos:** contrato entre serviços a manter; latência de rede (timeout e circuit breaker).
- **O que muda para quem desenvolve:** mudança de contrato altera os dois lados e `contracts/` no mesmo PR.
