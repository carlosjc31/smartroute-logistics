# 0005 — Identidade com Keycloak (OIDC + PKCE) e API como resource server

- **Status:** Aceita (registro retroativo da Fase 3)
- **Data:** 2026-11-27
- **Specs e issues:** #28, #29

## Contexto

A API estava aberta. A plataforma vai ter perfis distintos (ADMIN, DISPATCHER, DRIVER, CUSTOMER), vários
tenants e, no futuro, SSO com o diretório das transportadoras. Implementar login próprio traria riscos
(armazenamento de senha, força bruta, recuperação de conta).

## Decisão

- Keycloak como provedor de identidade, com o realm versionado em `infra/keycloak/`.
- Frontend como client público com Authorization Code + PKCE (S256); token em `sessionStorage`.
- backend-core como OAuth2 Resource Server stateless: valida assinatura (JWKS), `iss`, validade e
  audience `smartroute-api`. Perfis vêm de `realm_access.roles`.
- Rotas negadas por padrão; cada rota tem regra explícita por perfil.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Login próprio com Spring Security e senha no banco | Reinventa força bruta, política de senha, MFA e federação |
| Auth0, Cognito ou similar (SaaS) | Custo por usuário e dependência externa; dados de funcionários fora do controle da transportadora |
| Token em `localStorage` | Exposto a XSS por mais tempo; `sessionStorage` limita à aba |

## Consequências

- **Positivas:** MFA, federação (LDAP/AD) e brute-force prontos; a API não guarda senha.
- **Negativas e custos:** mais um serviço no compose; testes E2E fazem login real.
- **O que muda para quem desenvolve:** nunca implemente login; o tenant (Fase 4) virá de um claim do token.
