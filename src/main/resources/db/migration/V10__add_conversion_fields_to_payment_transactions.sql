ALTER TABLE payment_transactions
    ADD COLUMN sender_currency VARCHAR(10) NULL,
    ADD COLUMN receiver_currency VARCHAR(10) NULL,
    ADD COLUMN exchange_rate DECIMAL(19,10) NULL,
    ADD COLUMN transfer_charge DECIMAL(19,4) NULL,
    ADD COLUMN converted_amount DECIMAL(19,4) NULL;

