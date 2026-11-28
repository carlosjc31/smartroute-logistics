## O que muda

<!-- Resumo em duas ou três frases. -->

Closes #

## Spec e requisitos

- Spec: `specs/NNN-nome/` <!-- ou "não se aplica" (correção de bug, dependência, CI) com o motivo -->
- Requisitos cobertos: <!-- ex.: TEN-001, TEN-002 -->
- ADRs novas ou alteradas: <!-- ex.: docs/adr/0010-... -->

## Como foi testado

<!-- Comandos rodados e o que foi verificado manualmente. -->

## Checklist

- [ ] A spec estava `approved` antes do código (ou a mudança não altera comportamento)
- [ ] Testes novos marcados com `@req` e `python3 scripts/check_spec_traceability.py` passa
- [ ] Rota nova com regra no `SecurityConfig` e caso no `ApiSecurityTest`
- [ ] Alteração de dado de negócio auditada na mesma transação
- [ ] Nenhum dado pessoal (coordenadas, endereços, buscas) em log, erro ou auditoria
- [ ] Contrato (`contracts/`) e os dois lados atualizados juntos, se mudou
- [ ] Docs atualizadas (`CLAUDE.md`, `docs/`, índice de specs)
