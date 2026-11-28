# 0003 — Arquitetura hexagonal no backend-core

- **Status:** Aceita (registro retroativo da Fase 1)
- **Data:** 2026-11-27
- **Specs e issues:** #12, #13

## Contexto

O backend original misturava regras de negócio com JPA, HTTP e a chamada ao ai-engine, o que gerou bugs
(enum duplicado, NPE na validação, URL fixa) e tornava impossível testar o domínio sem banco.

## Decisão

Cada contexto do backend segue portas e adapters:

- `domain/`: records imutáveis que validam invariantes no construtor, sem Spring, JPA ou Jackson.
- `application/`: casos de uso e portas de saída (`port/out`); não importa `infrastructure`.
- `infrastructure/`: adapters que implementam as portas (JPA, REST, ai-engine, Nominatim, segurança).
- A API expõe DTOs, nunca entidades JPA nem records de domínio. `Clock` é injetado.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Camadas tradicionais (controller → service → repository JPA) | O domínio fica acoplado ao JPA e os testes precisam de banco |
| Clean Architecture completa (portas de entrada, *use case interactors*) | Mais cerimônia do que o tamanho do sistema justifica |

## Consequências

- **Positivas:** domínio testado sem infraestrutura; trocar Nominatim ou o ai-engine é trocar um adapter.
- **Negativas e custos:** mapeamento entre entidade, domínio e DTO.
- **O que muda para quem desenvolve:** ArchUnit vai garantir as dependências entre camadas a partir da
  R3 ([ADR 0004](0004-monolito-modular.md)).
