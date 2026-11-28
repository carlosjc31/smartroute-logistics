# Segurança

Como o SmartRoute protege a API, os serviços internos e os segredos. As regras que todo PR deve seguir
estão em [`CLAUDE.md`](../CLAUDE.md#segurança-regras-não-negociáveis).

## Autenticação e perfis (#28)

- **Keycloak** (OIDC) é o provedor de identidade. O realm `smartroute` fica versionado em
  [`infra/keycloak/smartroute-realm.json`](../infra/keycloak/smartroute-realm.json) e é importado ao
  subir o compose.
- O **frontend** é um client público (`smartroute-frontend`) com *Authorization Code + PKCE (S256)*:
  não há segredo no navegador. O token fica no `sessionStorage` e é renovado antes de expirar
  (duração de 5 minutos).
- O **backend-core** é um *OAuth2 Resource Server* stateless. Ele valida a assinatura (JWKS do Keycloak),
  o `iss`, a validade e a *audience* `smartroute-api`. Tokens emitidos para outros clients são
  recusados.
- Perfis vêm de `realm_access.roles`:

| Perfil | Acesso hoje |
|---|---|
| `ADMIN` | Viagens, geocodificação e auditoria (`/api/v1/audit/**`) |
| `DISPATCHER` | Viagens e geocodificação |
| `DRIVER`, `CUSTOMER` | Nenhum ainda. Precisam de vínculo com a viagem, que chega na Fase 4 (#38, #39) |

- Rotas públicas: só `/actuator/health` e, fora de produção, o Swagger. Qualquer rota não mapeada é
  negada.
- Erros de segurança seguem o RFC 7807: `401 Não autenticado` (com `WWW-Authenticate: Bearer`, sem dizer
  se o token expirou ou foi forjado) e `403 Acesso negado`.
- O realm tem proteção contra força bruta (bloqueio temporário após 5 falhas) e política de senha.

Usuários de desenvolvimento (senha em `KEYCLOAK_DEV_USERS_PASSWORD` do `.env`): `admin.dev`
(ADMIN + DISPATCHER), `despacho.dev` (DISPATCHER) e `motorista.dev` (DRIVER). **Em produção, remova a
seção `users` do realm** e crie os usuários pelo console ou por federação (LDAP/AD, SSO da empresa).

## CORS, headers e rate limit (#29)

- **CORS** só para as origens de `CORS_ALLOWED_ORIGINS` (a aplicação recusa `*` ao iniciar). Métodos
  `GET`, `POST`, `PATCH` e `OPTIONS`.
- **Headers**: `Content-Security-Policy` restritiva (`frame-ancestors 'none'`, `object-src 'none'`),
  `Strict-Transport-Security` (1 ano, em HTTPS), `X-Content-Type-Options: nosniff`,
  `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`. Erros nunca incluem mensagem interna nem
  stack trace.
- **Rate limit** (Bucket4j): `RATE_LIMIT_CAPACITY` requisições por minuto (padrão 120) por usuário
  (`sub` do token) ou por IP, se anônimo. Ao estourar, a resposta é `429` com `Retry-After`. O limite
  fica em memória, por instância; com várias réplicas, use Bucket4j sobre Redis. Em produção, o
  perfil `prod` usa `X-Forwarded-For` do proxy para descobrir o IP real.

## Serviço interno: ai-engine (#31)

- Só o backend-core chama o ai-engine, pela rede interna do compose. A porta 8000 **não** é publicada
  no host.
- Toda chamada a `/predict/eta` exige `Authorization: Bearer <AI_ENGINE_TOKEN>`, comparado em tempo
  constante. Sem `SERVICE_TOKEN`, o ai-engine nem sobe.
- O modelo `.pkl` só é carregado se o SHA-256 bater com `models/delivery_model.pkl.sha256`, gerado pelo
  `training/train_model.py`, ou com `MODEL_SHA256`. Um artefato adulterado deixa o serviço em `503`, e o
  backend usa a estimativa heurística.
- Pendente na #31: trocar o pickle por um formato que não executa código (skops ou ONNX).

## Segredos (#30)

- Nenhuma senha tem valor padrão versionado. O `docker compose` não sobe sem `POSTGRES_PASSWORD`,
  `AI_ENGINE_TOKEN`, `KEYCLOAK_ADMIN_PASSWORD`, `KEYCLOAK_DEV_USERS_PASSWORD` e
  `GRAFANA_ADMIN_PASSWORD` no `.env`. O perfil `prod` do backend falha se faltar qualquer variável.
- O CI gera segredos aleatórios a cada execução, o que prova que nada depende de valor fixo.
- **Produção**: injete os segredos a partir de um cofre (HashiCorp Vault, AWS Secrets Manager, Azure Key
  Vault ou GCP Secret Manager) como variáveis de ambiente ou arquivos montados, nunca em imagem ou
  repositório.
- **Rotação**:
  1. Gere o novo valor: `openssl rand -hex 32`.
  2. Atualize o cofre.
  3. Reinicie os serviços que usam o segredo. Para o token do ai-engine, reinicie ai-engine e backend
     juntos.
  4. Revogue o valor antigo. Para o banco, use `ALTER ROLE ... PASSWORD` e depois atualize o cofre.
     Rotacione imediatamente qualquer segredo que tenha aparecido em log, issue ou commit.
- O gitleaks varre todo o histórico em cada PR. As exceções (placeholders e tokens de teste) ficam em
  [`.gitleaks.toml`](../.gitleaks.toml).

## Containers (#32)

- Imagens com tag fixa, multi-stage, e usuário não-root (UID 10001) no ai-engine e no backend-core.
- No compose: `no-new-privileges` em todos os serviços. Nos serviços de aplicação, também
  `read_only` (com `/tmp` em tmpfs) e `cap_drop: ALL`. Healthchecks no banco, no Keycloak e no
  ai-engine.

## Varreduras no CI (#33)

| Ferramenta | O que verifica | Bloqueia o PR |
|---|---|---|
| gitleaks | Segredos em todo o histórico do git | Qualquer achado |
| Trivy | CVEs nas dependências e nas imagens; má configuração nos Dockerfiles | CVE crítica com correção; config alta/crítica |
| CodeQL | Análise estática de Java, Python, TypeScript e workflows | Alertas na aba *Security* |
| OWASP ZAP | DAST da API pelo OpenAPI, sobre o compose do E2E | Regras de nível FAIL |
| Dependabot | Atualizações de Maven, pip, npm, Docker e Actions | Abre PRs semanais |

As varreduras do Trivy e do CodeQL também rodam toda segunda-feira, porque CVEs novas aparecem mesmo
sem mudança no código.

Uma CVE só pode ser aceita no `.trivyignore` se o código vulnerável não for usado. A entrada precisa de
justificativa, de uma issue com a correção definitiva e de uma data de expiração (`exp:`). Quando a data
vence, o Trivy volta a bloquear o CI. Hoje há duas exceções no `spring-webmvc` 6.2.x (XsltView e SSE),
que dependem da migração para o Spring Boot 4 (#69).

## Auditoria (#35)

Criações e mudanças de status de viagens geram um registro em `audit_log` na mesma transação: ator
(`preferred_username` do token), data/hora, ação e estado antes/depois. O banco recusa `UPDATE`,
`DELETE` e `TRUNCATE` nessa tabela por trigger. Leitura: `GET /api/v1/audit/trips/{id}`, só para
`ADMIN`.

## Relatar uma vulnerabilidade

Não abra issue pública. Entre em contato diretamente com o mantenedor pelo perfil do GitHub.
