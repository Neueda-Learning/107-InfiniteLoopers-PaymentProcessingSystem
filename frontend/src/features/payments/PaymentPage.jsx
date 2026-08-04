import { useEffect, useMemo, useState } from 'react';
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

export function PaymentPage() {
  const [customers, setCustomers] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [loadingCustomers, setLoadingCustomers] = useState(true);
  const [apiError, setApiError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [receipt, setReceipt] = useState(null);

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

  function handleChange(event) {
    const { name, value } = event.target;
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

    const validationErrors = validatePaymentForm(form);
    setErrors(validationErrors);
    setApiError('');
    setSuccessMessage('');

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    setSubmitting(true);

    try {
      const response = await sendPayment({
        ...form,
        amount: Number(form.amount),
      });

      setReceipt(response);
      setSuccessMessage(response.message || 'Payment created successfully.');
      setForm((current) => ({ ...initialForm, senderAccountNumber: current.senderAccountNumber }));
    } catch (err) {
      setApiError(err.message || 'Unable to submit the payment.');
    } finally {
      setSubmitting(false);
    }
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
              />
              {errors.senderAccountNumber ? <small className="field-error">{errors.senderAccountNumber}</small> : null}
            </label>

            <label>
              <span>Receiver account number</span>
              <input
                name="receiverAccountNumber"
                value={form.receiverAccountNumber}
                onChange={handleChange}
                placeholder="100000000002"
              />
              {errors.receiverAccountNumber ? <small className="field-error">{errors.receiverAccountNumber}</small> : null}
            </label>

            <label>
              <span>Receiver IFSC code</span>
              <input
                name="receiverIfscCode"
                value={form.receiverIfscCode}
                onChange={handleChange}
                placeholder="HDFC0005678"
              />
            </label>

            <label>
              <span>Amount</span>
              <input
                name="amount"
                type="number"
                min="0"
                step="0.01"
                value={form.amount}
                onChange={handleChange}
                placeholder="2500"
              />
              {errors.amount ? <small className="field-error">{errors.amount}</small> : null}
            </label>

            <label className="form-grid__full">
              <span>Description</span>
              <textarea
                name="description"
                value={form.description}
                onChange={handleChange}
                rows="4"
                placeholder="Optional note for the transaction"
              />
              <div className="field-meta">
                <small>{form.description.length}/255 characters</small>
                {errors.description ? <small className="field-error">{errors.description}</small> : null}
              </div>
            </label>

            <label>
              <span>UPI PIN</span>
              <input
                name="upiPin"
                type="password"
                inputMode="numeric"
                maxLength="4"
                value={form.upiPin}
                onChange={handleChange}
                placeholder="1234"
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
                onClick={() => {
                  setForm(initialForm);
                  setErrors({});
                  setApiError('');
                  setSuccessMessage('');
                }}
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
                      senderAccountNumber: current.senderAccountNumber || customer.value,
                      receiverAccountNumber: current.senderAccountNumber === customer.value ? current.receiverAccountNumber : customer.value,
                      receiverIfscCode: customer.ifscCode,
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

