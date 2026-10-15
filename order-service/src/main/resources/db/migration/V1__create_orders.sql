CREATE TABLE orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    seat_id UUID NOT NULL,
    amount_cents BIGINT NOT NULL CHECK (amount_cents >= 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED')),
    payment_reference VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- "My orders" looks orders up by user.
CREATE INDEX ix_orders_user_id ON orders (user_id);

--  "Seat availability" looks orders up by seat.
CREATE UNIQUE INDEX ux_orders_paid_seat ON orders (seat_id) WHERE status = 'PAID';
