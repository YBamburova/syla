CREATE TABLE identity.accounts (
    id uuid PRIMARY KEY,
    issuer varchar(500) NOT NULL,
    subject varchar(255) NOT NULL,
    state varchar(16) NOT NULL CHECK (state IN ('ACTIVE','DELETING','DELETED')),
    created_at timestamptz NOT NULL,
    deleted_at timestamptz,
    UNIQUE (issuer, subject)
);
CREATE INDEX accounts_cleanup ON identity.accounts (state, deleted_at);
