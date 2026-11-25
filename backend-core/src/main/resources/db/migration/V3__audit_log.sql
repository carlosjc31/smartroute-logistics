-- Log de auditoria append-only (issue #35). Estados antes/depois sem coordenadas nem endereços (LGPD).
CREATE TABLE audit_log (
    id           UUID PRIMARY KEY,
    occurred_at  TIMESTAMPTZ  NOT NULL,
    actor        VARCHAR(200) NOT NULL,
    action       VARCHAR(50)  NOT NULL,
    entity_type  VARCHAR(50)  NOT NULL,
    entity_id    UUID         NOT NULL,
    before_state JSONB,
    after_state  JSONB        NOT NULL
);

CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id, occurred_at DESC);

-- Imutabilidade garantida pelo banco, não só pela aplicação
CREATE FUNCTION audit_log_reject_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_log é somente inserção (% não permitido)', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_log_immutable
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION audit_log_reject_change();

CREATE TRIGGER audit_log_no_truncate
    BEFORE TRUNCATE ON audit_log
    FOR EACH STATEMENT EXECUTE FUNCTION audit_log_reject_change();
