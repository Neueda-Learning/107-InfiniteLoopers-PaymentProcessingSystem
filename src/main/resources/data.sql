INSERT INTO customers (id, customer_name, email, phone_number) VALUES
    (1, 'Alice Johnson', 'alice.johnson@example.com', '9876543210'),
    (2, 'Bob Smith', 'bob.smith@example.com', '9876501234'),
    (3, 'Rahul Sharma', 'rahul.sharma@example.com', '9876502234'),
    (4, 'Priya Reddy', 'priya.reddy@example.com', '9876503234'),
    (5, 'John Wilson', 'john.wilson@example.com', '9876504234'),
    (6, 'Emma Thomas', 'emma.thomas@example.com', '9876505234'),
    (7, 'David Miller', 'david.miller@example.com', '9876506234'),
    (8, 'Sophia Brown', 'sophia.brown@example.com', '9876507234'),
    (9, 'Arjun Patel', 'arjun.patel@example.com', '9876508234'),
    (10, 'Neha Kapoor', 'neha.kapoor@example.com', '9876509234');

INSERT INTO accounts (id, account_number, ifsc_code, bank_name, balance, upi_pin, is_active, currency, daily_transaction_limit, customer_id) VALUES
    (1, '100000000001', 'SBIN0001234', 'State Bank of India', 50000.0000, '1234', TRUE, 'INR', 100000.0000, 1),
    (2, '100000000002', 'HDFC0005678', 'HDFC Bank', 30000.0000, '5678', TRUE, 'INR', 100000.0000, 2),
    (3, '100000000003', 'ICIC0002345', 'ICICI Bank', 185000.0000, '4321', TRUE, 'INR', 100000.0000, 1),
    (4, '100000000004', 'UTIB0003456', 'Axis Bank', 92000.0000, '8765', TRUE, 'INR', 100000.0000, 2),
    (5, '100000000005', 'SBIN0004567', 'State Bank of India', 125000.0000, '2468', TRUE, 'INR', 100000.0000, 3),
    (6, '100000000006', 'HDFC0006789', 'HDFC Bank', 54000.0000, '1357', TRUE, 'INR', 100000.0000, 3),
    (7, '100000000007', 'ICIC0007890', 'ICICI Bank', 87000.0000, '2244', TRUE, 'INR', 100000.0000, 4),
    (8, '100000000008', 'UTIB0008901', 'Axis Bank', 143500.0000, '6688', TRUE, 'INR', 100000.0000, 4),
    (9, '100000000009', 'PUNB0009012', 'PNB', 61000.0000, '1122', TRUE, 'USD', 1200.0000, 5),
    (10, '100000000010', 'BOFAUS3N001', 'Bank of America', 275000.0000, '3344', TRUE, 'USD', 1200.0000, 5),
    (11, '100000000011', 'CHASUS33A01', 'Chase', 158000.0000, '5566', TRUE, 'USD', 1200.0000, 6),
    (12, '100000000012', 'WFBIUS6S002', 'Wells Fargo', 48400.0000, '7788', TRUE, 'EUR', 1000.0000, 6),
    (13, '100000000013', 'HSBCINBB003', 'HSBC', 99000.0000, '9900', TRUE, 'USD', 1200.0000, 7),
    (14, '100000000014', 'BARCGB22IND', 'Barclays', 132750.0000, '4455', TRUE, 'EUR', 1000.0000, 7),
    (15, '100000000015', 'SBIN0001122', 'State Bank of India', 76500.0000, '6677', TRUE, 'USD', 1200.0000, 8),
    (16, '100000000016', 'HDFC0002211', 'HDFC Bank', 188900.0000, '8899', TRUE, 'EUR', 1000.0000, 8),
    (17, '100000000017', 'ICIC0003344', 'ICICI Bank', 111000.0000, '1212', TRUE, 'USD', 1200.0000, 9),
    (18, '100000000018', 'UTIB0004433', 'Axis Bank', 69000.0000, '3434', TRUE, 'EUR', 1000.0000, 9),
    (19, '100000000019', 'PUNB0005544', 'PNB', 97000.0000, '5656', TRUE, 'USD', 1200.0000, 10),
    (20, '100000000020', 'HSBCINBB020', 'HSBC', 205500.0000, '7878', TRUE, 'EUR', 1000.0000, 10);

INSERT INTO payment_transactions (
    id, transaction_id, sender_account_id, receiver_account_id, amount, description,
    payment_status, idempotency_key, retry_count, created_time, validated_time,
    sent_time, completed_time, failed_time
) VALUES
    (1, 'TXN-00000000000000000000000000000001', 1, 2, 1500.0000, 'Dummy successful transfer',
     'COMPLETED', 'IDEM-00000000000000000000000000000001', 0,
     TIMESTAMP '2026-08-03 10:00:00', TIMESTAMP '2026-08-03 10:00:05', TIMESTAMP '2026-08-03 10:00:07', TIMESTAMP '2026-08-03 10:00:10', NULL),
    (2, 'TXN-00000000000000000000000000000002', 2, 1, 700.0000, 'Dummy failed transfer',
     'FAILED', 'IDEM-00000000000000000000000000000002', 1,
     TIMESTAMP '2026-08-03 11:00:00', TIMESTAMP '2026-08-03 11:00:03', NULL, NULL, TIMESTAMP '2026-08-03 11:00:08');

INSERT INTO transaction_status_history (id, transaction_id, status, timestamp) VALUES
    (1, 1, 'CREATED',   TIMESTAMP '2026-08-03 10:00:00'),
    (2, 1, 'VALIDATED', TIMESTAMP '2026-08-03 10:00:05'),
    (3, 1, 'SENT',      TIMESTAMP '2026-08-03 10:00:07'),
    (4, 1, 'COMPLETED', TIMESTAMP '2026-08-03 10:00:10'),
    (5, 2, 'CREATED',   TIMESTAMP '2026-08-03 11:00:00'),
    (6, 2, 'VALIDATED', TIMESTAMP '2026-08-03 11:00:03'),
    (7, 2, 'FAILED',    TIMESTAMP '2026-08-03 11:00:08');

