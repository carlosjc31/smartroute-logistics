---
id: "004"
title: Acesso, segurança e erros da API
prefix: SEC
status: implemented
issues: [13, 28, 29]
module: shared
depends_on: []
---

# 004 — Acesso, segurança e erros da API

> Spec retroativa e transversal: vale para toda rota do backend e para o cliente HTTP do frontend.
> Specs de funcionalidade declaram só as regras de perfil das próprias rotas.

## Contexto

A API estava aberta e devolvia erros com stack trace. A plataforma vai ter vários perfis e, na Fase 4,
vários tenants. Decisões em [ADR 0005](../../docs/adr/0005-keycloak-oidc-pkce.md); operação em
[`docs/seguranca.md`](../../docs/seguranca.md).

## Objetivos

- Só usuários autenticados no Keycloak, com o perfil certo, acessam a API.
- Erros previsíveis (RFC 7807) e sem vazar detalhes internos.

## Fora do escopo

- Isolamento por tenant (#36). Rate limit distribuído (Redis). MFA obrigatório.

## Atores e perfis

`ADMIN`, `DISPATCHER`, `DRIVER` e `CUSTOMER`, vindos de `realm_access.roles` do token.

## Requisitos

### Autenticação e autorização

- **SEC-001** — A API deve aceitar só JWT com assinatura válida (JWKS do Keycloak), `iss` esperado,
  dentro da validade e com audience `smartroute-api`. Caso contrário, `401` em RFC 7807 com
  `WWW-Authenticate: Bearer`, sem dizer o motivo da recusa.
- **SEC-002** — Os perfis devem vir de `realm_access.roles`; perfis desconhecidos são ignorados e token
  sem perfil não tem autorização nenhuma.
- **SEC-003** — O ator registrado (auditoria, logs) deve ser o `preferred_username`, senão o `sub`,
  senão `system`.
- **SEC-004** — Toda rota não mapeada deve ser negada (`403` para autenticado). Só `/actuator/health`
  (e o Swagger fora de produção) são públicos.

### Proteções HTTP

- **SEC-010** — O CORS deve aceitar só as origens de `CORS_ALLOWED_ORIGINS`, com os métodos `GET`,
  `POST`, `PATCH` e `OPTIONS`. A aplicação não deve iniciar com a origem `*`.
- **SEC-011** — Toda resposta deve ter `Content-Security-Policy` com `frame-ancestors 'none'`,
  `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer` e, em HTTPS,
  `Strict-Transport-Security` de um ano.
- **SEC-012** — Cada usuário (ou IP, se anônimo) deve ter um limite de requisições por minuto
  (`RATE_LIMIT_CAPACITY`). Ao estourar, `429` com `Retry-After`, sem afetar outros usuários.

### Erros

- **SEC-020** — Erros inesperados devem responder `500` em RFC 7807 com mensagem genérica, sem a
  mensagem da exceção nem stack trace.

### Frontend

- **SEC-030** — O painel só deve ser exibido para usuário autenticado; sem sessão, redireciona ao login
  do Keycloak (Authorization Code + PKCE) guardando a página atual, sem redirecionar enquanto a sessão
  carrega. Erro no login é exibido com opção de tentar de novo. O usuário logado é exibido e pode sair.
- **SEC-031** — O cliente HTTP deve enviar o token da sessão em `Authorization` e nunca enviar token
  expirado.
- **SEC-032** — O cliente HTTP deve transformar respostas de erro (inclusive `401`) em `ApiError`, com a
  mensagem do servidor e os erros por campo; sem corpo, uma mensagem genérica; sem conexão, uma mensagem
  de falha de conexão.

## Critérios de aceite

```gherkin
Cenário: token emitido para outro serviço (SEC-001)
  Dado um token válido do Keycloak com audience "outro-servico"
  Quando ele chama GET /api/v1/trips
  Então a resposta é 401 com WWW-Authenticate "Bearer"

Cenário: rajada de requisições (SEC-012)
  Dado RATE_LIMIT_CAPACITY=5
  Quando o mesmo usuário faz 6 requisições no mesmo minuto
  Então a sexta recebe 429 com Retry-After, e outro usuário continua recebendo 200

Cenário: erro inesperado (SEC-020)
  Dado que o caso de uso lança uma exceção com "senha=abc" na mensagem
  Quando o cliente chama a rota
  Então a resposta é 500 "Erro interno" e o corpo não contém "senha=abc"
```

## Dados pessoais (LGPD)

| Dado | Titular | Base legal | Retenção | Quem acessa |
|---|---|---|---|---|
| Nome, e-mail e perfis | Funcionário | Execução de contrato | Enquanto houver vínculo | Keycloak; o backend só lê do token |
| IP (chave do rate limit anônimo) | Usuário | Legítimo interesse | 1 minuto, em memória | Ninguém |

## Questões em aberto

- [ ] Rate limit distribuído (Redis) antes de rodar mais de uma réplica.
