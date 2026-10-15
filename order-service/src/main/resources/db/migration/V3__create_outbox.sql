-- Outbox table to store messages to be sent to other services 

CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    topic VARCHAR(100) NOT NULL,
    message_key VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);

-- Index to quickly find unpublished messages
CREATE INDEX ix_outbox_unpublished ON outbox (created_at) WHERE published_at IS NULL;
