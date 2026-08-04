CREATE TABLE customers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_customers_email UNIQUE (email),
    CONSTRAINT uq_customers_phone_number UNIQUE (phone_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

