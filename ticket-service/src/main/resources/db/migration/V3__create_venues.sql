-- A stadium. Capacity is not stored: it is derived from the sections, so it can never disagree with them.
CREATE TABLE venues (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    city VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- A block of seats laid out as rows x seats_per_row. The price is the default copied onto each match's seats.
CREATE TABLE venue_sections (
    id UUID PRIMARY KEY,
    venue_id UUID NOT NULL REFERENCES venues (id) ON DELETE CASCADE,
    name VARCHAR(20) NOT NULL,
    row_count INTEGER NOT NULL CHECK (row_count > 0),
    seats_per_row INTEGER NOT NULL CHECK (seats_per_row > 0),
    price_cents BIGINT NOT NULL CHECK (price_cents >= 0),
    UNIQUE (venue_id, name)
);

-- Expand step: nullable for now because existing events have no venue yet. V4 backfills it and adds NOT NULL.
ALTER TABLE events ADD COLUMN venue_id UUID REFERENCES venues (id);

CREATE INDEX ix_events_venue_id ON events (venue_id);
