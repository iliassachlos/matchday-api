-- Demo data: five stadiums, their sections, ten matches, and every seat of every match.
-- Capacities are deliberately small so a sell-out is reachable in a demo or a k6 run.

INSERT INTO venues (id, name, city) VALUES
    (gen_random_uuid(), 'ΟΑΚΑ',       'Αθήνα'),
    (gen_random_uuid(), 'Καραϊσκάκη', 'Πειραιάς'),
    (gen_random_uuid(), 'Τούμπα',     'Θεσσαλονίκη'),
    (gen_random_uuid(), 'OPAP Arena', 'Αθήνα'),
    (gen_random_uuid(), 'Παγκρήτιο',  'Ηράκλειο');

-- Sections find their venue by name, so no generated UUID has to be copied by hand.
INSERT INTO venue_sections (id, venue_id, name, row_count, seats_per_row, price_cents)
SELECT gen_random_uuid(), v.id, s.name, s.row_count, s.seats_per_row, s.price_cents
FROM (VALUES
    ('ΟΑΚΑ',       'Θύρα 1',    20, 50,  3500),
    ('ΟΑΚΑ',       'Θύρα 7',    20, 50,  2500),
    ('ΟΑΚΑ',       'Θύρα 13',   18, 50,  2500),
    ('ΟΑΚΑ',       'VIP',        5, 20, 12000),
    ('Καραϊσκάκη', 'Θύρα 1',    15, 40,  3000),
    ('Καραϊσκάκη', 'Θύρα 4',    15, 40,  2500),
    ('Καραϊσκάκη', 'Θύρα 7',    18, 40,  2000),
    ('Καραϊσκάκη', 'VIP',        4, 20, 10000),
    ('Τούμπα',     'Θύρα 4',    15, 40,  2500),
    ('Τούμπα',     'Θύρα 5',    12, 40,  2000),
    ('Τούμπα',     'Θύρα 8',    10, 40,  2000),
    ('Τούμπα',     'VIP',        2, 10,  9000),
    ('OPAP Arena', 'Θύρα 21',   12, 40,  3000),
    ('OPAP Arena', 'Θύρα 22',   12, 40,  2500),
    ('OPAP Arena', 'Θύρα 23',    6, 35,  2500),
    ('OPAP Arena', 'VIP',        3, 10, 11000),
    ('Παγκρήτιο',  'Ανατολική', 10, 35,  1500),
    ('Παγκρήτιο',  'Δυτική',    10, 35,  1500),
    ('Παγκρήτιο',  'Βόρεια',     5, 18,  1200),
    ('Παγκρήτιο',  'VIP',        1, 10,  6000)
) AS s (venue, name, row_count, seats_per_row, price_cents)
JOIN venues v ON v.name = s.venue;

INSERT INTO events (id, venue_id, name, starts_at)
SELECT gen_random_uuid(), v.id, m.name, m.starts_at::timestamptz
FROM (VALUES
    ('ΟΑΚΑ',       'Ελλάδα – Ολλανδία',          '2026-11-12 21:45+02'),
    ('ΟΑΚΑ',       'Ελλάδα – Ιταλία',          '2026-11-15 21:45+02'),
    ('Καραϊσκάκη', 'Ολυμπιακός – Παναθηναϊκός',  '2026-10-25 19:30+02'),
    ('Καραϊσκάκη', 'Ολυμπιακός – ΑΕΚ',           '2026-12-06 19:30+02'),
    ('Τούμπα',     'ΠΑΟΚ – Άρης',                '2026-11-01 19:30+02'),
    ('Τούμπα',     'ΠΑΟΚ – Ολυμπιακός',          '2026-11-29 20:00+02'),
    ('OPAP Arena', 'ΑΕΚ – ΠΑΟΚ',                 '2026-11-08 20:00+02'),
    ('OPAP Arena', 'ΑΕΚ – Παναθηναϊκός',         '2026-12-13 20:00+02'),
    ('Παγκρήτιο',  'ΟΦΗ – Ατρόμητος',            '2026-11-22 17:00+02'),
    ('Παγκρήτιο',  'Ελλάδα – Κύπρος',            '2026-12-20 19:00+02')
) AS m (venue, name, starts_at)
JOIN venues v ON v.name = m.venue;

-- Unfold every section's template into concrete seats, once per match:
-- for each event, for each section of its venue, for each row, for each seat in the row.
-- Unfold every section's template into concrete seats, once per match:
-- for each event, for each section of its venue, for each row, for each seat in the row.
INSERT INTO seats (id, event_id, section, row_label, seat_number, price_cents, status)
SELECT gen_random_uuid(), e.id, vs.name, r::text, n, vs.price_cents, 'AVAILABLE'
FROM events e
JOIN venue_sections vs ON vs.venue_id = e.venue_id,
     generate_series(1, vs.row_count) AS r,
     generate_series(1, vs.seats_per_row) AS n;

