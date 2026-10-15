-- One ticket per paid order, issued when order-service announces the payment.

CREATE TABLE tickets (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE,
    seat_id UUID NOT NULL REFERENCES seats (id),
    user_id UUID NOT NULL,
    holder_email VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('VALID', 'USED')),
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    used_at TIMESTAMPTZ,
    CONSTRAINT ck_tickets_used_has_time CHECK ((status = 'USED') = (used_at IS NOT NULL))
);
