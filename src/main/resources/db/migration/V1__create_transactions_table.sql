CREATE TABLE transactions
(
    transaction_id VARCHAR(50) PRIMARY KEY,
    nsu            VARCHAR(50)    NOT NULL,
    terminal_id    VARCHAR(50)    NOT NULL,
    amount         DECIMAL(19, 2) NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Garantia de idempotência
ALTER TABLE transactions
    ADD CONSTRAINT uk_terminal_nsu UNIQUE (terminal_id, nsu);