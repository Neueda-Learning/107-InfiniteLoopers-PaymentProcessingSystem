-- Align account currency support with the current requirements.
-- Existing migrations already introduced the column, so this migration standardizes
-- the column definition and refreshes dummy account currencies.

ALTER TABLE accounts
    MODIFY COLUMN currency VARCHAR(10) NOT NULL DEFAULT 'INR';

UPDATE accounts
SET currency = 'INR'
WHERE bank_name IN ('State Bank of India', 'HDFC Bank', 'ICICI Bank', 'Axis Bank', 'PNB');

UPDATE accounts
SET currency = 'USD'
WHERE bank_name IN ('Bank of America', 'Chase', 'Wells Fargo');

UPDATE accounts
SET currency = 'GBP'
WHERE bank_name IN ('HSBC', 'Barclays');

UPDATE accounts
SET currency = 'EUR'
WHERE bank_name IN ('Deutsche Bank');

