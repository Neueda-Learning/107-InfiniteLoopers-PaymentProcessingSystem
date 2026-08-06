INSERT INTO customers (id, customer_name, email, phone_number) VALUES
    (3, 'Rahul Sharma', 'rahul.sharma@example.com', '9876502234'),
    (4, 'Priya Reddy', 'priya.reddy@example.com', '9876503234'),
    (5, 'John Wilson', 'john.wilson@example.com', '9876504234'),
    (6, 'Emma Thomas', 'emma.thomas@example.com', '9876505234'),
    (7, 'David Miller', 'david.miller@example.com', '9876506234'),
    (8, 'Sophia Brown', 'sophia.brown@example.com', '9876507234'),
    (9, 'Arjun Patel', 'arjun.patel@example.com', '9876508234'),
    (10, 'Neha Kapoor', 'neha.kapoor@example.com', '9876509234');

INSERT INTO accounts (id, account_number, ifsc_code, bank_name, balance, upi_pin, is_active, customer_id) VALUES
    (3, '100000000003', 'ICIC0002345', 'ICICI Bank', 185000.0000, '4321', b'1', 1),
    (4, '100000000004', 'UTIB0003456', 'Axis Bank', 92000.0000, '8765', b'1', 2),
    (5, '100000000005', 'SBIN0004567', 'State Bank of India', 125000.0000, '2468', b'1', 3),
    (6, '100000000006', 'HDFC0006789', 'HDFC Bank', 54000.0000, '1357', b'1', 3),
    (7, '100000000007', 'ICIC0007890', 'ICICI Bank', 87000.0000, '2244', b'1', 4),
    (8, '100000000008', 'UTIB0008901', 'Axis Bank', 143500.0000, '6688', b'1', 4),
    (9, '100000000009', 'PUNB0009012', 'PNB', 61000.0000, '1122', b'1', 5),
    (10, '100000000010', 'BOFAUS3N001', 'Bank of America', 275000.0000, '3344', b'1', 5),
    (11, '100000000011', 'CHASUS33A01', 'Chase', 158000.0000, '5566', b'1', 6),
    (12, '100000000012', 'WFBIUS6S002', 'Wells Fargo', 48400.0000, '7788', b'1', 6),
    (13, '100000000013', 'HSBCINBB003', 'HSBC', 99000.0000, '9900', b'1', 7),
    (14, '100000000014', 'BARCGB22IND', 'Barclays', 132750.0000, '4455', b'1', 7),
    (15, '100000000015', 'SBIN0001122', 'State Bank of India', 76500.0000, '6677', b'1', 8),
    (16, '100000000016', 'HDFC0002211', 'HDFC Bank', 188900.0000, '8899', b'1', 8),
    (17, '100000000017', 'ICIC0003344', 'ICICI Bank', 111000.0000, '1212', b'1', 9),
    (18, '100000000018', 'UTIB0004433', 'Axis Bank', 69000.0000, '3434', b'1', 9),
    (19, '100000000019', 'PUNB0005544', 'PNB', 97000.0000, '5656', b'1', 10),
    (20, '100000000020', 'HSBCINBB020', 'HSBC', 205500.0000, '7878', b'1', 10);

