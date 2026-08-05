# Payment Processing System - Backend Tasks

## Tasks

### 1. Audit Trail for Support Staff
- Add audit logging for all failed validations and payment issues.
- Track events like:
  - Wrong UPI PIN
  - Invalid account details
  - Payment failures
  - Validation errors
- Support staff should be able to view failure reasons and timestamps.

---

### 2. Real-Time Account Number Validation
- Validate account number while the user is typing.
- Handle:
  - Non-numeric input
  - Minimum/maximum account number length
  - Invalid account formats
- Show errors instantly without waiting for the send button.

---

### 3. Payment Cancellation Window
- Provide a 5-second window after clicking "Send Payment".
- Show a "Cancel Payment" option during this period.
- Update transaction status accordingly if cancelled.

---

### 4. Duplicate Payment Detection
- Detect repeated payments with:
  - Same sender
  - Same receiver
  - Same amount
- Show confirmation popup:
  > "You have already made this payment. Do you want to continue?"

---

### 5. Multiple Account Support
- Allow a single user to create multiple accounts.
- Provide separate dashboards for each account.
- Each dashboard should show:
  - Balance
  - Transactions
  - Account details

---

## Good To Have

### Currency Conversion
- Add currency conversion support.
- Show latest exchange rates before international payments.
