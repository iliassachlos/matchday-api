-- Start the purchase test with no orders, so the invariant check counts only this run
DELETE FROM outbox;
DELETE FROM orders;
