# Meeting Notes

| Field | Details |
|---|---|
| **Meeting** | Second Client Meeting - UI Review & Feature Discussion |
| **Date** | 6 August 2026 |
| **Attendees** | Client, Development Team |
| **Purpose** | Review UI walkthrough and finalize feature requirements for the Payment Processing System |

## 1. UI Walkthrough

The team presented the UI to the client. The client reviewed the screens and confirmed the UI design is approved as-is; no changes were requested to the visual layout.

## 2. Client Feedback & Decisions

### 2.1 Real-Time Account Number Validation

The client asked that the account number field validate input as the user types, rather than allowing incorrect entries to pass through and only failing later at payment submission. Specifically:

- The field should not accept invalid characters/format at all while typing (not accept and then fail on send).
- Non-numeric input should be blocked.
- Minimum/maximum account number length should be enforced live.
- Errors should be shown instantly, without waiting for the "Send" action.

### 2.2 Email Alert for Payments > ₹10,000 — Not Feasible at This Time

The team informed the client that automated email generation for payments above ₹10,000 could not be implemented in the current phase.

### 2.3 New Feature — Popup Alert for Payments > ₹10,000

In place of the email alert, the client proposed a simpler alternative: show an on-screen popup warning whenever a payment greater than ₹10,000 is initiated. Agreed scope:

- Trigger a popup alert when the payment amount exceeds ₹10,000.
- Popup should give the customer a "Cancel Payment" option.
- Transaction status should be updated accordingly if the customer cancels.

### 2.4 Multiple Account Support

The client requested that a single customer be able to create and manage multiple accounts. Agreed scope:

- Allow one user to create multiple accounts.
- Provide a separate dashboard per account.
- Each dashboard should display balance, transactions, and account details.

## 3. Full Backend Task List (Confirmed)

The following items remain part of the confirmed development scope, in addition to the feedback above:

| Item | Details |
|---|---|
| **Audit Trail for Support Staff** | Log all failed validations and payment issues (wrong UPI PIN, invalid account details, payment failures, validation errors). Support staff should be able to view failure reasons and timestamps. |
| **Real-Time Account Number Validation** | Validate account number as the user types — handle non-numeric input, min/max length, and invalid formats. Show errors instantly, without waiting for Send. |
| **Payment Cancellation Window** | Provide a 5-second window after "Send Payment" is clicked, with a "Cancel Payment" option. Update transaction status accordingly if cancelled. |
| **Payment > ₹10,000 Alert** | Show a popup alert to the customer when a payment exceeds ₹10,000, with a "Cancel Payment" option. Update transaction status accordingly if cancelled. |
| **Duplicate Payment Detection** | Detect repeated payments with the same sender, receiver, and amount. Show a confirmation popup: "You have already made this payment. Do you want to continue?" |
| **Multiple Account Support** | Allow a single user to create multiple accounts, each with its own dashboard showing balance, transactions, and account details. |

## 4. Good to Have

- **Currency Conversion** — add support for currency conversion and display latest exchange rates before international payments.

## 5. Action Items

- Implement real-time account number validation as described in 2.1.
- Build popup alert + cancel flow for payments > ₹10,000 as described in 2.3.
- Design and implement multiple account support with per-account dashboards as described in 2.4.
- Continue development of remaining confirmed tasks listed in Section 3.
- Currency conversion remains a good-to-have, to be scheduled if time/budget allows.
