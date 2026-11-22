-- Schema original (criado antes pelo Hibernate com ddl-auto=update).
-- IF NOT EXISTS mantém a migration segura para bancos que já tinham a tabela.
CREATE TABLE IF NOT EXISTS trips (
    id                UUID PRIMARY KEY,
    origin_lat        DOUBLE PRECISION,
    origin_lng        DOUBLE PRECISION,
    dest_lat          DOUBLE PRECISION,
    dest_lng          DOUBLE PRECISION,
    payload_weight    DOUBLE PRECISION,
    status            VARCHAR(255),
    predicted_arrival TIMESTAMP(6),
    created_at        TIMESTAMP(6),
    departure_time    TIMESTAMP(6)
);
