-- Reset and seed data for the load smoke test

DELETE FROM seats;
DELETE FROM events;

INSERT INTO events (id, name, starts_at, created_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Greece vs Netherlands',
    '2026-10-13 19:00:00+03',
    now()
);

-- 100 seats in one block: section 14, row 8, numbers 1..100

INSERT INTO seats (id, event_id, section, row_label, seat_number, price_cents, status, created_at)
SELECT
    gen_random_uuid(),
    e.id,
    '14',
    '8',
    n,
    4500,
    'AVAILABLE',
    now()
FROM events e, generate_series(1, 100) AS n
WHERE e.name = 'Greece vs Netherlands';
