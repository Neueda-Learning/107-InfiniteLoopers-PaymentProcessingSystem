CREATE TABLE accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_number VARCHAR(20) NOT NULL,
    ifsc_code VARCHAR(11) NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    balance DECIMAL(19,4) NOT NULL,
    upi_pin VARCHAR(255) NOT NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    customer_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_accounts_account_number UNIQUE (account_number),
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

