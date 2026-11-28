# 0004 — Monólito modular com critérios de extração de serviço

- **Status:** Aceita (implementação na etapa R3)
- **Data:** 2026-11-27
- **Specs e issues:** Fase 4 (#36 a #43)

## Contexto

Hoje o backend é um único módulo com camadas horizontais (`domain/`, `application/`, `infrastructure/`).
A Fase 4 traz cinco contextos novos (tenant, frota, motoristas, clientes e pedidos, viagens com
paradas), fortemente acoplados pelo fluxo de alocação: viagem, veículo, motorista e jornada são
validados juntos. Várias regras dependem de uma transação e de um banco: auditoria na mesma transação
(constituição, art. 6), RLS por tenant e validação de alocação. O projeto tem uma pessoa mantenedora e
ainda não tem carga real.

Microsserviços foram considerados para a reestruturação e descartados por ora (tabela abaixo).

## Decisão

1. O backend-core vira um **monólito modular** com [Spring Modulith](https://spring.io/projects/spring-modulith):
   um pacote de topo por contexto (`trip`, `geocoding`, `audit`, `tenant`, `fleet`, ...), cada um
   hexagonal por dentro ([ADR 0003](0003-arquitetura-hexagonal.md)), mais `shared` para segurança e
   utilidades transversais.
2. Fronteiras verificadas em teste (`ApplicationModules.verify()`) e regras hexagonais com ArchUnit.
3. Para que extrair seja barato depois:
   - cada módulo tem schema próprio no PostgreSQL e não lê tabela de outro módulo;
   - comunicação entre módulos só pela API pública do módulo ou por eventos de domínio, com o
     *event publication registry* do Modulith (outbox transacional).
4. **Critérios de extração.** Um módulo só vira serviço separado, por nova ADR, quando atender a pelo
   menos um:
   - precisa escalar de forma independente (perfil de carga muito diferente do restante);
   - precisa de stack ou runtime diferente;
   - tem time próprio com ciclo de deploy próprio;
   - falha nele não pode afetar a API principal (isolamento de disponibilidade).
5. Candidatos já identificados, a avaliar nas etapas em que surgem: **tracking** (ingestão de GPS,
   4.4/4.5) e **notificações** (consumidor do outbox, #43). O ai-engine já é separado
   ([ADR 0006](0006-ai-engine-servico-separado.md)).

## Alternativas consideradas

| Alternativa | Por que não agora |
|---|---|
| Microsserviços por contexto desde a R3 | Exige broker, gateway, autenticação entre serviços, tracing, saga em vez de transação e RLS em cada banco antes de qualquer funcionalidade nova; atrasa a Fase 4 em duas ou três etapas; o ganho (escala e times independentes) ainda não existe |
| Manter camadas horizontais | Com cinco contextos novos, `domain/` e `application/` viram um amontoado sem fronteiras |
| Módulos Maven separados | Fronteira forte, mas build mais lento e refatoração cara; o Modulith verifica o mesmo em teste |

## Consequências

- **Positivas:** uma transação e um deploy; fronteiras explícitas e testadas; extração futura vira
  mover o pacote e trocar o evento em memória por um broker.
- **Negativas e custos:** disciplina para não atravessar fronteiras (o teste do Modulith ajuda);
  consultas que cruzam módulos precisam de API pública ou de modelo de leitura.
- **O que muda para quem desenvolve:** código novo entra no pacote do módulo dono; nada de
  `import ...outromodulo.internal`.
