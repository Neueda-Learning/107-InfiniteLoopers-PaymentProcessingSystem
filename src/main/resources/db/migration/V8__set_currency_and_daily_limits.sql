-- Migration V8: Update daily transaction limits and currency for accounts

-- Update V5 dummy data accounts with INR currency and default limit
UPDATE accounts SET currency = 'INR', daily_transaction_limit = 100000.0000
WHERE id IN (1, 2);

-- Update V6 dummy data accounts with appropriate currencies and limits
-- Customers 3-6: Indian customers - INR with 100,000 limit (ids 3-10)
UPDATE accounts SET currency = 'INR', daily_transaction_limit = 100000.0000
WHERE id IN (3, 4, 5, 6, 7, 8);

-- Customers 7-8: International customers - USD with 1200 limit (ids 9-10)
UPDATE accounts SET currency = 'USD', daily_transaction_limit = 1200.00
WHERE id IN (9, 10);

-- Customers 9-10: International customers - USD and EUR with appropriate limits (ids 11-20)
UPDATE accounts SET currency = 'USD', daily_transaction_limit = 1200.00
WHERE id IN (11, 13, 15, 17, 19);

UPDATE accounts SET currency = 'EUR', daily_transaction_limit = 1000.00
WHERE id IN (12, 14, 16, 18, 20);

