-- A sold seat remembers the order that bought it.
ALTER TABLE seats ADD COLUMN order_id UUID;

-- A seat is sold if and only if it has an order.
ALTER TABLE seats ADD CONSTRAINT ck_seats_sold_has_order
    CHECK ((status = 'SOLD') = (order_id IS NOT NULL));

-- One order buys exactly one seat.
CREATE UNIQUE INDEX ux_seats_order_id ON seats (order_id);
