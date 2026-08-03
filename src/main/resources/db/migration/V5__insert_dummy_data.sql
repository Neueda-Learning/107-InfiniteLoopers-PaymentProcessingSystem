INSERT INTO customers (id, customer_name, email, phone_number) VALUES
    (1, 'Alice Johnson', 'alice.johnson@example.com', '9876543210'),
    (2, 'Bob Smith', 'bob.smith@example.com', '9876501234');

INSERT INTO accounts (id, account_number, ifsc_code, bank_name, balance, upi_pin, is_active, customer_id) VALUES
    (1, '100000000001', 'SBIN0001234', 'State Bank of India', 50000.0000, '1234', b'1', 1),
    (2, '100000000002', 'HDFC0005678', 'HDFC Bank', 30000.0000, '5678', b'1', 2);

INSERT INTO payment_transactions (
    id, transaction_id, sender_account_id, receiver_account_id, amount, description,
    payment_status, idempotency_key, retry_count, created_time, validated_time,
    sent_time, completed_time, failed_time
) VALUES
    (1, 'TXN-00000000000000000000000000000001', 1, 2, 1500.0000, 'Dummy successful transfer',
     'COMPLETED', 'IDEM-00000000000000000000000000000001', 0,
     '2026-08-03 10:00:00', '2026-08-03 10:00:05', '2026-08-03 10:00:07', '2026-08-03 10:00:10', NULL),
    (2, 'TXN-00000000000000000000000000000002', 2, 1, 700.0000, 'Dummy failed transfer',
     'FAILED', 'IDEM-00000000000000000000000000000002', 1,
     '2026-08-03 11:00:00', '2026-08-03 11:00:03', NULL, NULL, '2026-08-03 11:00:08');

INSERT INTO transaction_status_history (transaction_id, status, timestamp) VALUES
    (1, 'CREATED',   '2026-08-03 10:00:00'),
    (1, 'VALIDATED', '2026-08-03 10:00:05'),
    (1, 'SENT',      '2026-08-03 10:00:07'),
    (1, 'COMPLETED', '2026-08-03 10:00:10'),
    (2, 'CREATED',   '2026-08-03 11:00:00'),
    (2, 'VALIDATED', '2026-08-03 11:00:03'),
    (2, 'FAILED',    '2026-08-03 11:00:08');

