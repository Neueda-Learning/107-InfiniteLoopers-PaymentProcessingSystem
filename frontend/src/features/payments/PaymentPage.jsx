import { useEffect, useMemo, useRef, useState } from 'react';
import { getAllCustomers } from '../../api/customerApi';
import { sendPayment } from '../../api/paymentApi';
import {
  ErrorAlert,
  LoadingState,
  PageHeader,
  SectionCard,
  StatusBadge,
  SuccessAlert,
} from '../../components/UI';
import { formatCurrency, formatDateTime } from '../../utils/formatters';
import { validatePaymentForm } from '../../utils/validators';

const initialForm = {
  senderAccountNumber: '',
  receiverAccountNumber: '',
  receiverIfscCode: '',
  amount: '',
  description: '',
  upiPin: '',
};

const ACCOUNT_NUMBER_PATTERN = /^\d+$/;
const IFSC_PATTERN = /^[A-Z]{4}0[A-Z0-9]{6}$/;

export function PaymentPage() {
  const [customers, setCustomers] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [loadingCustomers, setLoadingCustomers] = useState(true);
  const [apiError, setApiError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [receipt, setReceipt] = useState(null);
  const [largeAmountWarningOpen, setLargeAmountWarningOpen] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [pendingPayload, setPendingPayload] = useState(null);
  const submissionGuard = useRef(false);

  useEffect(() => {
    let active = true;

    async function loadCustomers() {
      try {
        const response = await getAllCustomers();
        if (active) {
          setCustomers(response);
        }
      } catch (err) {
        if (active) {
          setApiError(err.message || 'Unable to load customer suggestions.');
        }
      } finally {
        if (active) {
          setLoadingCustomers(false);
        }
      }
    }

    loadCustomers();
    return () => {
      active = false;
    };
  }, []);

  const customerOptions = useMemo(() => {
    return customers.map((customer) => ({
      label: `${customer.customerName} • ${customer.accountNumber}`,
      value: customer.accountNumber,
      ifscCode: customer.ifscCode,
      bankName: customer.bankName,
      name: customer.customerName,
      balance: customer.balance,
    }));
  }, [customers]);

  const customerByAccount = useMemo(() => {
    return customerOptions.reduce((acc, customer) => {
      acc[customer.value] = customer;
      return acc;
    }, {});
  }, [customerOptions]);

  const knownAccounts = useMemo(() => new Set(customerOptions.map((customer) => customer.value)), [customerOptions]);

  const senderAccount = form.senderAccountNumber.trim();
  const receiverAccount = form.receiverAccountNumber.trim();
  const receiverIfsc = form.receiverIfscCode.trim().toUpperCase();

  const senderAccountValid = senderAccount && ACCOUNT_NUMBER_PATTERN.test(senderAccount) && knownAccounts.has(senderAccount);
  const receiverAccountValid =
    receiverAccount
    && ACCOUNT_NUMBER_PATTERN.test(receiverAccount)
    && knownAccounts.has(receiverAccount)
    && receiverAccount !== senderAccount;
  const receiverExpectedIfsc = receiverAccountValid ? customerByAccount[receiverAccount]?.ifscCode?.toUpperCase() : '';
  const receiverIfscValid =
    IFSC_PATTERN.test(receiverIfsc)
    && (!receiverExpectedIfsc || receiverIfsc === receiverExpectedIfsc);

  const canEnterReceiver = Boolean(senderAccountValid);
  const canEnterIfsc = Boolean(receiverAccountValid);
  const canEnterRemaining = Boolean(receiverAccountValid && receiverIfscValid);

  function showStageAlert(message) {
    setApiError(message);
    setSuccessMessage('');
  }

  function blockIfLocked(isLocked, message, event) {
    if (!isLocked) {
      return;
    }

    event.preventDefault();
    showStageAlert(message);
  }

  function handleChange(event) {
    const { name } = event.target;
    let { value } = event.target;

    if (name === 'senderAccountNumber' || name === 'receiverAccountNumber') {
      value = value.replace(/\D/g, '');
    }

    if (name === 'receiverIfscCode') {
      value = value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 11);
    }

    if (name === 'upiPin') {
      value = value.replace(/\D/g, '').slice(0, 4);
    }

    setForm((current) => ({ ...current, [name]: value }));
    setErrors((current) => ({ ...current, [name]: '' }));
    setApiError('');
    setSuccessMessage('');
  }

  function applySampleTransaction() {
    if (customerOptions.length < 2) {
      return;
    }

    setForm({
      senderAccountNumber: customerOptions[0].value,
      receiverAccountNumber: customerOptions[1].value,
      receiverIfscCode: customerOptions[1].ifscCode,
      amount: '2500',
      description: 'Sample payment from frontend',
      upiPin: '1234',
    });
    setErrors({});
    setApiError('');
    setSuccessMessage('Sample data applied. You can submit directly or edit the values.');
  }

  async function handleSubmit(event) {
    event.preventDefault();

    const validationErrors = validatePaymentForm(form, {
      knownAccounts: Array.from(knownAccounts),
      receiverExpectedIfsc,
    });
    setErrors(validationErrors);
    setApiError('');
    setSuccessMessage('');

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    const amount = Number(form.amount);
    const payload = {
      ...form,
      amount,
    };

    if (amount > 10000) {
      setPendingPayload(payload);
      setLargeAmountWarningOpen(true);
      return;
    }

    setPendingPayload(payload);
    setConfirmOpen(true);
  }

  function cancelPendingSubmission() {
    setConfirmOpen(false);
    setPendingPayload(null);
    submissionGuard.current = false;
    setSuccessMessage('Payment canceled.');
  }

  function handleLargeAmountWarningConfirm() {
    setLargeAmountWarningOpen(false);
    setConfirmOpen(true);
  }

  function handleLargeAmountWarningCancel() {
    setLargeAmountWarningOpen(false);
    setPendingPayload(null);
    setSuccessMessage('Payment canceled.');
  }

  async function proceedWithPendingSubmission() {
    if (!pendingPayload || submissionGuard.current) {
      return;
    }

    submissionGuard.current = true;
    setConfirmOpen(false);
    setSubmitting(true);

    try {
      const response = await sendPayment(pendingPayload);

      setReceipt(response);
      setSuccessMessage(response.message || 'Payment created successfully.');

      setForm((current) => ({ ...initialForm, senderAccountNumber: current.senderAccountNumber }));
      setPendingPayload(null);
    } catch (err) {
      setApiError(err.message || 'Unable to submit the payment.');
    } finally {
      setSubmitting(false);
      submissionGuard.current = false;
    }
  }

  useEffect(() => {
    return () => {
      setLargeAmountWarningOpen(false);
      setConfirmOpen(false);
      setPendingPayload(null);
      submissionGuard.current = false;
    };
  }, []);

  function handleResetForm() {
    setForm(initialForm);
    setErrors({});
    setApiError('');
    setSuccessMessage('');
    setLargeAmountWarningOpen(false);
    setConfirmOpen(false);
    setPendingPayload(null);
    submissionGuard.current = false;
  }

  function getConfirmationSummary() {
    if (!pendingPayload) {
      return '';
    }

    return `${pendingPayload.senderAccountNumber} → ${pendingPayload.receiverAccountNumber} (${formatCurrency(pendingPayload.amount)})`;
  }

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Money transfer"
        title="Send payment"
        description="Create a new payment request using the same fields expected by the backend `PaymentRequest` DTO."
        actions={
          <button type="button" className="secondary-button" onClick={applySampleTransaction}>
            Use sample data
          </button>
        }
      />

      <ErrorAlert message={apiError} onDismiss={() => setApiError('')} />
      <SuccessAlert message={successMessage} onDismiss={() => setSuccessMessage('')} />

      {largeAmountWarningOpen ? (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.7)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
          }}
        >
          <div
            style={{
              backgroundColor: '#0f172a',
              borderRadius: '24px',
              border: '2px solid #f59e0b',
              padding: '32px',
              maxWidth: '500px',
              boxShadow: '0 20px 60px rgba(0, 0, 0, 0.8), inset 0 0 0 1px rgba(245, 158, 11, 0.3)',
            }}
          >
            <h2 style={{ margin: '0 0 16px 0', fontSize: '1.5rem', color: '#fbbf24' }}>⚠️ Large Amount Warning</h2>
            <p style={{ margin: '0 0 24px 0', color: '#cbd5e1', fontSize: '1rem' }}>
              You are about to transfer a large amount of <strong style={{ color: '#fbbf24' }}>{formatCurrency(pendingPayload?.amount || 0)}</strong>.
            </p>
            <p style={{ margin: '0 0 24px 0', color: '#cbd5e1', fontSize: '0.95rem' }}>
              Please confirm that you want to proceed with this transaction.
            </p>
            <div
              style={{
                display: 'flex',
                gap: '12px',
              }}
            >
              <button
                type="button"
                onClick={handleLargeAmountWarningConfirm}
                style={{
                  flex: 1,
                  padding: '12px 20px',
                  backgroundColor: '#f59e0b',
                  color: '#1f2937',
                  border: 'none',
                  borderRadius: '12px',
                  fontSize: '1rem',
                  fontWeight: '600',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                }}
              >
                ✓ Yes, Continue
              </button>
              <button
                type="button"
                onClick={handleLargeAmountWarningCancel}
                style={{
                  flex: 1,
                  padding: '12px 20px',
                  backgroundColor: '#ef4444',
                  color: '#ffffff',
                  border: 'none',
                  borderRadius: '12px',
                  fontSize: '1rem',
                  fontWeight: '600',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                }}
              >
                ✕ Cancel Transfer
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {confirmOpen ? (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.7)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
          }}
        >
          <div
            style={{
              backgroundColor: '#0f172a',
              borderRadius: '24px',
              border: '2px solid #2563eb',
              padding: '32px',
              maxWidth: '500px',
              boxShadow: '0 20px 60px rgba(0, 0, 0, 0.8), inset 0 0 0 1px rgba(37, 99, 235, 0.3)',
            }}
          >
            <h2 style={{ margin: '0 0 16px 0', fontSize: '1.5rem', color: '#f8fafc' }}>Confirm Payment</h2>
            <p style={{ margin: '0 0 24px 0', color: '#cbd5e1', fontSize: '1rem' }}>
              Please confirm this transaction:
            </p>
            <div
              style={{
                backgroundColor: 'rgba(37, 99, 235, 0.15)',
                border: '1px solid rgba(37, 99, 235, 0.3)',
                borderRadius: '16px',
                padding: '16px',
                marginBottom: '24px',
                fontFamily: 'monospace',
                fontSize: '0.95rem',
                color: '#93c5fd',
                wordBreak: 'break-all',
              }}
            >
              {getConfirmationSummary()}
            </div>
            <div
              style={{
                display: 'flex',
                gap: '12px',
              }}
            >
              <button
                type="button"
                onClick={proceedWithPendingSubmission}
                disabled={submitting}
                style={{
                  flex: 1,
                  padding: '12px 20px',
                  backgroundColor: '#22c55e',
                  color: '#ffffff',
                  border: 'none',
                  borderRadius: '12px',
                  fontSize: '1rem',
                  fontWeight: '600',
                  cursor: submitting ? 'not-allowed' : 'pointer',
                  opacity: submitting ? 0.6 : 1,
                  transition: 'all 0.2s ease',
                }}
              >
                ✓ Confirm
              </button>
              <button
                type="button"
                onClick={cancelPendingSubmission}
                disabled={submitting}
                style={{
                  flex: 1,
                  padding: '12px 20px',
                  backgroundColor: '#ef4444',
                  color: '#ffffff',
                  border: 'none',
                  borderRadius: '12px',
                  fontSize: '1rem',
                  fontWeight: '600',
                  cursor: submitting ? 'not-allowed' : 'pointer',
                  opacity: submitting ? 0.6 : 1,
                  transition: 'all 0.2s ease',
                }}
              >
                ✕ Cancel
              </button>
            </div>
          </div>
        </div>
      ) : null}

      <div className="content-grid content-grid--2col">
        <SectionCard title="Payment form" subtitle="Validated on the client before calling `POST /api/payments/send`.">
          <form className="form-grid" onSubmit={handleSubmit}>
            <label>
              <span>Sender account number</span>
              <input
                name="senderAccountNumber"
                value={form.senderAccountNumber}
                onChange={handleChange}
                placeholder="100000000001"
                inputMode="numeric"
              />
              {errors.senderAccountNumber ? <small className="field-error">{errors.senderAccountNumber}</small> : null}
            </label>

            <label onMouseDown={(event) => blockIfLocked(!canEnterReceiver, 'Enter a valid sender account number first.', event)}>
              <span>Receiver account number</span>
              <input
                name="receiverAccountNumber"
                value={form.receiverAccountNumber}
                onChange={handleChange}
                placeholder="100000000002"
                inputMode="numeric"
                disabled={!canEnterReceiver}
              />
              {errors.receiverAccountNumber ? <small className="field-error">{errors.receiverAccountNumber}</small> : null}
            </label>

            <label onMouseDown={(event) => blockIfLocked(!canEnterIfsc, 'Enter a valid receiver account number first.', event)}>
              <span>Receiver IFSC code</span>
              <input
                name="receiverIfscCode"
                value={form.receiverIfscCode}
                onChange={handleChange}
                placeholder="HDFC0005678"
                disabled={!canEnterIfsc}
              />
              {errors.receiverIfscCode ? <small className="field-error">{errors.receiverIfscCode}</small> : null}
            </label>

            <label onMouseDown={(event) => blockIfLocked(!canEnterRemaining, 'Enter a valid IFSC code for the receiver account first.', event)}>
              <span>Amount</span>
              <input
                name="amount"
                type="number"
                min="0"
                step="0.01"
                value={form.amount}
                onChange={handleChange}
                placeholder="2500"
                disabled={!canEnterRemaining}
              />
              {errors.amount ? <small className="field-error">{errors.amount}</small> : null}
            </label>

            <label className="form-grid__full" onMouseDown={(event) => blockIfLocked(!canEnterRemaining, 'Enter a valid IFSC code for the receiver account first.', event)}>
              <span>Description</span>
              <textarea
                name="description"
                value={form.description}
                onChange={handleChange}
                rows="4"
                placeholder="Optional note for the transaction"
                disabled={!canEnterRemaining}
              />
              <div className="field-meta">
                <small>{form.description.length}/255 characters</small>
                {errors.description ? <small className="field-error">{errors.description}</small> : null}
              </div>
            </label>

            <label onMouseDown={(event) => blockIfLocked(!canEnterRemaining, 'Enter a valid IFSC code for the receiver account first.', event)}>
              <span>UPI PIN</span>
              <input
                name="upiPin"
                type="password"
                inputMode="numeric"
                maxLength="4"
                value={form.upiPin}
                onChange={handleChange}
                placeholder="1234"
                disabled={!canEnterRemaining}
              />
              {errors.upiPin ? <small className="field-error">{errors.upiPin}</small> : null}
            </label>

            <div className="form-grid__full button-row">
              <button type="submit" className="primary-button" disabled={submitting}>
                {submitting ? 'Submitting...' : 'Send payment'}
              </button>
              <button
                type="button"
                className="ghost-button"
                onClick={handleResetForm}
              >
                Reset
              </button>
            </div>
          </form>
        </SectionCard>

        <SectionCard title="Known customer accounts" subtitle="Useful for demoing against the seeded backend data.">
          {loadingCustomers ? (
            <LoadingState label="Loading customers..." />
          ) : customerOptions.length ? (
            <div className="list-stack">
              {customerOptions.map((customer) => (
                <button
                  key={customer.value}
                  type="button"
                  className="inline-card inline-card--button"
                  onClick={() => {
                    setForm((current) => ({
                      ...current,
                      senderAccountNumber: customer.value,
                    }));
                  }}
                >
                  <div>
                    <strong>{customer.name}</strong>
                    <p>{customer.bankName}</p>
                  </div>
                  <div className="text-right">
                    <span>{customer.value}</span>
                    <p>{formatCurrency(customer.balance)}</p>
                  </div>
                </button>
              ))}
            </div>
          ) : (
            <p className="muted">No customer accounts available yet.</p>
          )}
        </SectionCard>
      </div>

      <SectionCard title="Latest payment receipt" subtitle="Response from the backend `PaymentResponse` DTO.">
        {receipt ? (
          <div className="receipt-grid">
            <div>
              <span className="receipt-label">Transaction ID</span>
              <strong>{receipt.transactionId}</strong>
            </div>
            <div>
              <span className="receipt-label">Status</span>
              <StatusBadge status={receipt.paymentStatus} />
            </div>
            <div>
              <span className="receipt-label">Amount</span>
              <strong>{formatCurrency(receipt.amount)}</strong>
            </div>
            <div>
              <span className="receipt-label">Transaction time</span>
              <strong>{formatDateTime(receipt.transactionTime)}</strong>
            </div>
            <div>
              <span className="receipt-label">Sender</span>
              <strong>{receipt.senderAccountNumber}</strong>
            </div>
            <div>
              <span className="receipt-label">Receiver</span>
              <strong>{receipt.receiverAccountNumber}</strong>
            </div>
            <div className="receipt-message">
              <span className="receipt-label">Message</span>
              <p>{receipt.message}</p>
            </div>
          </div>
        ) : (
          <p className="muted">Submit a payment to see a confirmation receipt here.</p>
        )}
      </SectionCard>
    </div>
  );
}

