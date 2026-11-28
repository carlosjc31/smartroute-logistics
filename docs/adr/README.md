# Architecture Decision Records (ADRs)

Decisões de arquitetura que alguém precisaria conhecer para não desfazê-las por engano. Formato baseado
no [MADR](https://adr.github.io/madr/), em [`template.md`](template.md).

Regras:
- Uma decisão por arquivo, `NNNN-titulo-curto.md`, numeração sequencial.
- ADR aceita não é reescrita: se a decisão mudar, crie uma nova ADR e marque a antiga como
  `Substituída por`.
- Specs e planos citam as ADRs em que se apoiam; ADR nova entra no mesmo PR do plano que a motivou.

| ADR | Decisão | Status |
|---|---|---|
| [0001](0001-registrar-decisoes-em-adr.md) | Registrar decisões de arquitetura em ADRs | Aceita |
| [0002](0002-spec-driven-development.md) | Desenvolvimento orientado a especificação (SDD) | Aceita |
| [0003](0003-arquitetura-hexagonal.md) | Arquitetura hexagonal no backend-core | Aceita |
| [0004](0004-monolito-modular.md) | Monólito modular com critérios de extração de serviço | Aceita (implementada na R3) |
| [0005](0005-keycloak-oidc-pkce.md) | Identidade com Keycloak (OIDC + PKCE) e API como resource server | Aceita |
| [0006](0006-ai-engine-servico-separado.md) | Inferência de ETA em serviço Python separado | Aceita |
| [0007](0007-excecoes-de-cve-com-expiracao.md) | Exceções de CVE só com justificativa e expiração | Aceita |
| [0008](0008-websocket-para-tempo-real.md) | WebSocket (STOMP) em vez de SSE para tempo real | Aceita |
| [0009](0009-api-contract-first.md) | API contract-first com OpenAPI e geração de código | Aceita (implementada na R2) |
