-- Migration V11: Add failure_reason column to payment_transactions
-- Stores the human-readable reason why a transaction failed (wrong PIN, insufficient balance, daily limit exceeded)

ALTER TABLE payment_transactions
    ADD COLUMN failure_reason VARCHAR(1000) NULL;

