-- Tickets point at seats, so clear them before the seats they belong to are reset
DELETE FROM tickets;

-- Reset all seats to AVAILABLE status (order_id too: a seat has an order only while SOLD)
UPDATE seats
SET status = 'AVAILABLE', reserved_by = NULL, reserved_until = NULL, order_id = NULL, version = version + 1
WHERE status <> 'AVAILABLE';
