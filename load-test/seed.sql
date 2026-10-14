-- Reset all seats to AVAILABLE status
UPDATE seats
SET status = 'AVAILABLE', reserved_by = NULL, reserved_until = NULL, version = version + 1
WHERE status <> 'AVAILABLE';