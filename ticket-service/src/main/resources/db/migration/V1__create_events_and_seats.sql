-- Matches that tickets are sold for.
CREATE TABLE events (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- One row per physical seat, per event.
CREATE TABLE seats (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    section VARCHAR(20) NOT NULL,
    row_label VARCHAR(10) NOT NULL,
    seat_number INTEGER NOT NULL,
    price_cents BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    reserved_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- A physical seat exists exactly once per event.
CREATE UNIQUE INDEX ux_seats_event_location
    ON seats (event_id, section, row_label, seat_number);
