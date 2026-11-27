# 0008 — WebSocket (STOMP) em vez de SSE para tempo real

- **Status:** Aceita (implementação na etapa 4.5)
- **Data:** 2026-11-27
- **Specs e issues:** #42, [ADR 0007](0007-excecoes-de-cve-com-expiracao.md)

## Contexto

O mapa em tempo real (#42) precisa enviar posições e mudanças de status ao painel. SSE seria a opção mais
simples, mas o suporte do `spring-webmvc` 6.2.x tem uma CVE aceita (CVE-2026-47890) justamente porque a
API não expõe SSE.

## Decisão

Usamos WebSocket com STOMP (`spring-websocket`), autenticado com o mesmo JWT do Keycloak no `CONNECT`,
com tópicos por tenant. SSE só poderá ser reavaliado depois da migração para o Spring Boot 4 (#69).

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Server-Sent Events | Ativaria o caminho vulnerável da CVE-2026-47890 |
| Polling do frontend | Latência e carga desnecessárias com muitos veículos |

## Consequências

- **Positivas:** bidirecional (o PWA do motorista pode usar o mesmo canal); sem caminho vulnerável.
- **Negativas e custos:** autenticação e autorização por mensagem; proxies precisam permitir upgrade.
- **O que muda para quem desenvolve:** eventos de tempo real descritos em `contracts/events/` (AsyncAPI).
