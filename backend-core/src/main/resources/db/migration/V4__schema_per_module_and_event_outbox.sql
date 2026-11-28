-- Monólito modular (ADR 0004): cada módulo tem schema próprio e não lê tabela de outro.
-- SET SCHEMA move a tabela com dados, índices, constraints e triggers; nada é copiado.
CREATE SCHEMA IF NOT EXISTS trip;
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS events;

ALTER TABLE public.trips SET SCHEMA trip;

ALTER TABLE public.audit_log SET SCHEMA audit;
-- A função das triggers de imutabilidade acompanha a tabela (as triggers referenciam a função pelo OID)
ALTER FUNCTION public.audit_log_reject_change() SET SCHEMA audit;

-- Outbox: registro de publicação de eventos do Spring Modulith (spring.modulith.events.jdbc.schema=events).
-- DDL igual à do spring-modulith-events-jdbc 1.4 para PostgreSQL; a inicialização automática fica desligada.
-- Os eventos não carregam dados pessoais (só ids, status e datas).
CREATE TABLE events.event_publication (
    id               UUID                     NOT NULL,
    listener_id      TEXT                     NOT NULL,
    event_type       TEXT                     NOT NULL,
    serialized_event TEXT                     NOT NULL,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date  TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id)
);
CREATE INDEX event_publication_serialized_event_hash_idx ON events.event_publication USING hash (serialized_event);
CREATE INDEX event_publication_by_completion_date_idx ON events.event_publication (completion_date);
