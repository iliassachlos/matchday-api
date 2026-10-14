-- A reservation is a temporary hold: it expires unless it is paid for.
ALTER TABLE seats ADD COLUMN reserved_until TIMESTAMPTZ;

-- Holds taken before expiry existed get one that has already passed, so the release job frees them.
UPDATE seats SET reserved_until = now() WHERE status = 'RESERVED';

-- A seat is held if and only if it has an expiry. The database refuses any row that breaks this.
ALTER TABLE seats ADD CONSTRAINT ck_seats_hold_has_expiry
    CHECK ((status = 'RESERVED') = (reserved_until IS NOT NULL));

-- The release job only ever looks at held seats, so index only those rows.
CREATE INDEX ix_seats_hold_expiry ON seats (reserved_until) WHERE status = 'RESERVED';
