CREATE TABLE transaction_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    transaction_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    timestamp DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_status_history_transaction FOREIGN KEY (transaction_id) REFERENCES payment_transactions (id) ON DELETE CASCADE,
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

