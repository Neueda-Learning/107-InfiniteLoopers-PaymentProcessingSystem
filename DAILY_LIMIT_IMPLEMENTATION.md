# Daily Transaction Limit Implementation - Documentation

## Overview
This document explains the implementation of transaction failure categorization based on two checks:
1. **Insufficient Balance Check**: Transaction fails if sender's balance < transaction amount
2. **Daily Transaction Limit Check**: Transaction fails if total transactions (completed) today + current amount > daily limit

---

## Implementation Details

### 1. New Enum: Currency
**File**: `Currency.java`
- Defines supported currencies: INR, USD, EUR, GBP
- Each currency has a default daily transaction limit:
  - **INR** (Indian Rupee): ₹100,000 (1,00,000)
  - **USD** (US Dollar): $1,200
  - **EUR** (Euro): €1,000
  - **GBP** (British Pound): £950

### 2. Updated Account Model
**File**: `Account.java`
- Added `currency` field (Enum, defaults to INR)
- Added `dailyTransactionLimit` field (BigDecimal, defaults to 100,000.00)
- These allow per-account customization of currency and transaction limits

### 3. New Exception
**File**: `DailyTransactionLimitExceededException.java`
- Thrown when a transaction would exceed the daily limit for an account

### 4. New Validator
**File**: `DailyTransactionLimitValidator.java`
- Validates that a payment doesn't exceed the daily transaction limit
- Logic:
  ```
  1. Get all COMPLETED transactions from sender account for today (00:00:00 to 23:59:59)
  2. Sum their amounts
  3. If (sum + current_amount) > daily_limit, throw exception
  ```

### 5. Updated Repository
**File**: `PaymentTransactionRepository.java`
- Added new query method:
  ```java
  findCompletedTransactionsBySenderAccountOnDate(Long senderAccountId, LocalDateTime start, LocalDateTime end)
  ```
  - Retrieves only COMPLETED transactions within the date-time range
  - Used by DailyTransactionLimitValidator for limit calculations

### 6. Updated Payment Service
**File**: `PaymentServiceImpl.java`
- Injected `DailyTransactionLimitValidator`
- Added validation call after balance check:
  ```
  Step 5: Validate sender balance
  Step 5.1: Validate daily transaction limit  ← NEW
  Step 6: Validate UPI PIN
  ```
- Added `DailyTransactionLimitExceededException` to exception handling
- Transactions that fail either check are marked as FAILED

### 7. Updated Exception Handler
**File**: `GlobalExceptionHandler.java`
- Added handler for `DailyTransactionLimitExceededException`
- Returns HTTP 422 (Unprocessable Entity)
- Error code: `DAILY_TRANSACTION_LIMIT_EXCEEDED`

### 8. Updated Error Codes
**File**: `ErrorCode.java`
- Added `DAILY_TRANSACTION_LIMIT_EXCEEDED`

### 9. Database Migrations
**File**: `V7__add_currency_and_daily_limit_to_accounts.sql`
- Adds `currency` column (VARCHAR(3), default 'INR')
- Adds `daily_transaction_limit` column (DECIMAL(19,4), default 100000.0000)
- Creates index on currency column
- Adds check constraint to ensure limit is positive

**File**: `V8__set_currency_and_daily_limits.sql`
- Sets appropriate currencies and daily limits for all existing dummy accounts
- INR accounts: 100,000 limit
- USD accounts: 1,200 limit
- EUR accounts: 1,000 limit

---

## Analysis of Existing Dummy Transactions

### Current Dummy Transactions (from V5__insert_dummy_data.sql)

#### Transaction 1 (COMPLETED)
- **Transaction ID**: TXN-00000000000000000000000000000001
- **From**: Account 1 (Alice) - Balance: 50,000 INR
- **To**: Account 2 (Bob) - Balance: 30,000 INR
- **Amount**: 1,500 INR
- **Status**: COMPLETED
- **Analysis**:
  - ✅ Sufficient Balance: 50,000 > 1,500 ✓
  - ✅ Daily Limit: No prior transactions today, 1,500 < 100,000 ✓
  - ✅ Would PASS both new validation checks

#### Transaction 2 (FAILED)
- **Transaction ID**: TXN-00000000000000000000000000000002
- **From**: Account 2 (Bob) - Balance: 30,000 INR
- **To**: Account 1 (Alice)
- **Amount**: 700 INR
- **Status**: FAILED (retry_count = 1)
- **Analysis**:
  - ✅ Sufficient Balance: 30,000 > 700 ✓
  - ✅ Daily Limit: 700 < 100,000 ✓
  - ✅ Would PASS both new validation checks
  
  **Conclusion**: This transaction was marked as FAILED in the dummy data for **testing purposes only**. It does NOT fail due to insufficient balance or daily limit exceeded. The reason for the failure is not documented in the current schema. It was likely:
  - Used for testing retry logic
  - Or used for testing the failed status transition
  - Or used to demonstrate how FAILED status history is recorded

---

## Validation Order in Payment Processing

When a payment is initiated, validations occur in this order:

1. **Request Validation** - PaymentValidator
   - Check request fields are not null/empty
   - Check amount is positive

2. **Idempotency Check** - PaymentServiceImpl
   - Detect duplicate requests using idempotency key

3. **Account Validation** - AccountValidator
   - Verify sender account exists and is active
   - Verify receiver account exists and is active
   - Verify accounts are not the same

4. **Balance Validation** - BalanceValidator ✓ Already Implemented
   - Check sender has sufficient balance for the transaction amount
   - **Exception**: InsufficientBalanceException → HTTP 422
   - **Transaction Status**: FAILED

5. **Daily Limit Validation** - DailyTransactionLimitValidator ✓ NEW
   - Check total COMPLETED transactions today + current amount ≤ daily limit
   - **Exception**: DailyTransactionLimitExceededException → HTTP 422
   - **Transaction Status**: FAILED

6. **UPI PIN Validation** - PaymentServiceImpl
   - Verify the provided PIN matches the stored account PIN
   - **Exception**: InvalidUpiPinException → HTTP 401

7. **Transaction State Machine** - StatusTransitionValidator
   - Validate state transitions (CREATED → VALIDATED → SENT → COMPLETED)

---

## Error Response Examples

### Insufficient Balance
```json
{
  "timestamp": "2026-08-05T10:30:45",
  "status": 422,
  "error": "Unprocessable Entity",
  "errorCode": "INSUFFICIENT_FUNDS",
  "message": "Insufficient balance in account [100000000002]. Available: 5000.0000, Required: 10000.0000",
  "path": "/api/v1/payment/send-money"
}
```

### Daily Transaction Limit Exceeded
```json
{
  "timestamp": "2026-08-05T10:30:45",
  "status": 422,
  "error": "Unprocessable Entity",
  "errorCode": "DAILY_TRANSACTION_LIMIT_EXCEEDED",
  "message": "Daily transaction limit exceeded for account [100000000001]. Total sent today: 85000.0000, Requested amount: 20000.0000, Daily limit: 100000.0000 (Indian Rupee)",
  "path": "/api/v1/payment/send-money"
}
```

---

## Testing the Implementation

### Test Case 1: Insufficient Balance
```
Sender Account: 100000000001 (Balance: 1,000 INR)
Receiver Account: 100000000002
Amount: 5,000 INR
Expected Result: FAILED (InsufficientBalanceException)
```

### Test Case 2: Daily Limit Exceeded
```
Sender Account: 100000000001 (Balance: 200,000 INR, Daily Limit: 100,000 INR)
Previous Transactions Today: 80,000 INR (COMPLETED)
Amount: 30,000 INR
Expected Result: FAILED (DailyTransactionLimitExceededException)
Reason: 80,000 + 30,000 = 110,000 > 100,000 limit
```

### Test Case 3: Both Checks Pass
```
Sender Account: 100000000001 (Balance: 200,000 INR, Daily Limit: 100,000 INR)
Previous Transactions Today: 60,000 INR (COMPLETED)
Amount: 25,000 INR
Expected Result: COMPLETED
Reason: Balance OK (200,000 > 25,000) AND Daily limit OK (60,000 + 25,000 = 85,000 < 100,000)
```

### Test Case 4: Currency-Based Limits
```
Sender Account: 100000000011 (Balance: 158,000 USD, Currency: USD, Daily Limit: 1,200 USD)
Amount: 500 USD
Expected Result: Depends on previous transactions
- If no transactions today: COMPLETED
- If 900 USD already sent: FAILED (Daily limit exceeded)
```

---

## Database Schema Changes

### Before (V2)
```sql
CREATE TABLE accounts (
    id BIGINT,
    account_number VARCHAR(20),
    ifsc_code VARCHAR(11),
    bank_name VARCHAR(100),
    balance DECIMAL(19,4),
    upi_pin VARCHAR(255),
    is_active BIT(1),
    customer_id BIGINT,
    ...
)
```

### After (V7 + V8)
```sql
CREATE TABLE accounts (
    id BIGINT,
    account_number VARCHAR(20),
    ifsc_code VARCHAR(11),
    bank_name VARCHAR(100),
    balance DECIMAL(19,4),
    upi_pin VARCHAR(255),
    is_active BIT(1),
    currency VARCHAR(3),                    ← NEW
    daily_transaction_limit DECIMAL(19,4),  ← NEW
    customer_id BIGINT,
    ...
)
```

---

## Configuration & Customization

### Per-Account Customization
Each account can have:
- **Different Currency**: INR, USD, EUR, GBP
- **Custom Daily Limit**: Any positive BigDecimal value

### Global Defaults
Update the Currency enum to change default daily limits:
```java
public enum Currency {
    INR("Indian Rupee", new BigDecimal("100000.00")),  // Customize here
    USD("US Dollar", new BigDecimal("1200.00")),
    // ...
}
```

### Runtime Account Configuration
Update account settings via a new API endpoint (future enhancement):
```
PATCH /api/v1/accounts/{accountId}
{
    "currency": "USD",
    "dailyTransactionLimit": "5000.00"
}
```

---

## Summary

✅ **Implemented**:
- Currency field for accounts
- Daily transaction limit field for accounts
- DailyTransactionLimitValidator
- New exception and error code
- Database migrations
- Exception handling and response
- Proper validation order in payment processing

✅ **Transaction Failure Categorization**:
- **Insufficient Balance** → InsufficientBalanceException
- **Daily Limit Exceeded** → DailyTransactionLimitExceededException
- Both trigger transaction status = FAILED

✅ **Existing Dummy Transaction 2 Analysis**:
- Not failing due to insufficient balance or daily limit
- Was likely created for testing retry/failure logic
- Can be updated with real failure reasons in future versions

