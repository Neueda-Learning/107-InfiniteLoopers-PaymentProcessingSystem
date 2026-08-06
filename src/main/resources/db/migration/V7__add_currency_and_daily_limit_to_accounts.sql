-- Migration V7: Add currency and daily_transaction_limit columns to accounts table
-- Note: This migration recreates the accounts table to add new columns (H2-compatible)

ALTER TABLE accounts ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'INR';
ALTER TABLE accounts ADD COLUMN daily_transaction_limit DECIMAL(19,4) NOT NULL DEFAULT 100000.0000;

-- Create an index on currency for faster queries
CREATE INDEX idx_accounts_currency ON accounts(currency);


