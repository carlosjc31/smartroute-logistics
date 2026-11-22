-- Datas com fuso horário. Valores antigos (sem fuso) são interpretados como UTC.
ALTER TABLE trips
    ALTER COLUMN predicted_arrival TYPE TIMESTAMPTZ USING predicted_arrival AT TIME ZONE 'UTC',
    ALTER COLUMN created_at        TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN departure_time    TYPE TIMESTAMPTZ USING departure_time AT TIME ZONE 'UTC';

-- Ciclo de vida e metadados da previsão
ALTER TABLE trips
    ADD COLUMN IF NOT EXISTS actual_arrival    TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS eta_model_version VARCHAR(100),
    ADD COLUMN IF NOT EXISTS eta_source        VARCHAR(20),
    ADD COLUMN IF NOT EXISTS origin_label      VARCHAR(200),
    ADD COLUMN IF NOT EXISTS destination_label VARCHAR(200);

UPDATE trips SET eta_source = 'MODEL' WHERE eta_source IS NULL AND predicted_arrival IS NOT NULL;
UPDATE trips SET created_at = NOW() WHERE created_at IS NULL;

ALTER TABLE trips
    ALTER COLUMN origin_lat     SET NOT NULL,
    ALTER COLUMN origin_lng     SET NOT NULL,
    ALTER COLUMN dest_lat       SET NOT NULL,
    ALTER COLUMN dest_lng       SET NOT NULL,
    ALTER COLUMN payload_weight SET NOT NULL,
    ALTER COLUMN status         SET NOT NULL,
    ALTER COLUMN departure_time SET NOT NULL,
    ALTER COLUMN created_at     SET NOT NULL,
    ALTER COLUMN status         TYPE VARCHAR(20);

-- O Hibernate criava um CHECK sem o status CANCELLED
ALTER TABLE trips DROP CONSTRAINT IF EXISTS trips_status_check;
ALTER TABLE trips ADD CONSTRAINT trips_status_check
    CHECK (status IN ('PENDING', 'IN_TRANSIT', 'DELAYED', 'COMPLETED', 'CANCELLED'));
ALTER TABLE trips ADD CONSTRAINT trips_eta_source_check
    CHECK (eta_source IS NULL OR eta_source IN ('MODEL', 'FALLBACK'));

CREATE INDEX IF NOT EXISTS idx_trips_created_at ON trips (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_trips_status_created_at ON trips (status, created_at DESC);
