-- Empty until the ticket email has gone out. Lets a repeated announcement retry a failed email without issuing a second ticket.
ALTER TABLE tickets ADD COLUMN emailed_at TIMESTAMPTZ;
