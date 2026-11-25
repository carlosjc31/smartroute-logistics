# LGPD: mapeamento de dados pessoais

Ponto de partida da conformidade com a Lei 13.709/2018 (issue #34). O mapa precisa ser revisto a cada
funcionalidade que trate dados pessoais, em especial na Fase 4 (motoristas, clientes, rastreamento
GPS).

## Dados tratados hoje

| Dado | Onde | Titular | Pessoal? | Finalidade |
|---|---|---|---|---|
| Origem/destino (coordenadas e rótulo) | `trips` | Cliente ou destinatário, quando é um endereço | Sim, se for endereço de pessoa física | Calcular rota e ETA |
| Busca de geocodificação (`q`) | Cache em memória (24 h); enviada ao Nominatim | Idem | Idem | Converter endereço em coordenadas |
| Usuário (nome, e-mail, perfis) | Keycloak | Funcionário da transportadora | Sim | Autenticação e controle de acesso |
| Ator da auditoria (`preferred_username`) | `audit_log` | Funcionário | Sim | Rastreabilidade (segurança e prestação de contas) |
| Peso, horários, status, ETA | `trips`, `audit_log` | — | Não, isoladamente | Operação e re-treino do modelo |

## Bases legais (art. 7º)

- **Execução de contrato** (V): dados da entrega necessários para transportar a carga.
- **Legítimo interesse** (IX): auditoria de acesso e prevenção a fraudes, com avaliação de impacto
  documentada antes de produção.
- **Obrigação legal** (II): documentos fiscais (NF-e, CT-e, MDF-e) a partir da Fase 4.

## Medidas já implementadas

- **Minimização**: a auditoria guarda só campos operacionais (status, horários, peso, ETA). Coordenadas
  e endereços ficam de fora, como garante o `AuditServiceTest`.
- **Logs** não registram coordenadas, endereços nem a busca de geocodificação. Erros de dependência
  externa não expõem o conteúdo da requisição.
- **Controle de acesso** por perfil (veja [`seguranca.md`](seguranca.md)); a auditoria só é lida por
  `ADMIN`.
- **Transporte**: HSTS em produção. **Segredos** fora do repositório.
- **Compartilhamento**: o Nominatim (OpenStreetMap Foundation) recebe o texto buscado. Isso precisa
  constar na política de privacidade. Na Fase 5, o OSRM self-hosted reduz esse envio.

## Pendências (issue #34)

- [ ] **Retenção**: definir prazos (ex.: viagens identificáveis por 5 anos por exigência fiscal,
      depois anonimizar coordenadas e rótulos) e um job de anonimização.
- [ ] **Direitos do titular** (art. 18): endpoints de exportação e de exclusão/anonimização, que
      dependem do cadastro de clientes e motoristas (#38, #39).
- [ ] **Rastreamento GPS** (#41): consentimento e transparência para motoristas, com coleta só durante
      a jornada.
- [ ] Registro das operações de tratamento (art. 37), indicação do encarregado (DPO) e RIPD
      (relatório de impacto).
- [ ] Política de privacidade e termos de uso no painel.
