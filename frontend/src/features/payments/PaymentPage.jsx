import { useEffect, useMemo, useState } from 'react';
import { getAccountByNumber, getCustomerAccounts, getCustomers } from '../../api/customerApi';
import { previewPayment, sendPayment } from '../../api/paymentApi';
import { ErrorAlert, LoadingState, PageHeader, SuccessAlert } from '../../components/UI';
import { CustomerSelector } from './CustomerSelector';
import { PaymentConfirmation } from './PaymentConfirmation';
import { PaymentPreview } from './PaymentPreview';
import { PaymentSuccess } from './PaymentSuccess';
import { ReceiverDetails } from './ReceiverDetails';
import { SenderAccountDetails } from './SenderAccountDetails';

function parseAmount(value) {
  const amount = Number(value);
  return Number.isFinite(amount) ? amount : NaN;
}

function isValidUpiPin(value) {
  return /^\d{4}$/.test(value);
}

export function PaymentPage() {
  const [customers, setCustomers] = useState([]);
  const [accounts, setAccounts] = useState([]);

  const [selectedCustomerId, setSelectedCustomerId] = useState('');
  const [selectedSenderAccountNumber, setSelectedSenderAccountNumber] = useState('');
  const [receiverAccountNumber, setReceiverAccountNumber] = useState('');
  const [receiver, setReceiver] = useState(null);

  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [upiPin, setUpiPin] = useState('');

  const [preview, setPreview] = useState(null);
  const [result, setResult] = useState(null);

  const [loadingCustomers, setLoadingCustomers] = useState(true);
  const [loadingAccounts, setLoadingAccounts] = useState(false);
  const [loadingReceiver, setLoadingReceiver] = useState(false);
  const [previewing, setPreviewing] = useState(false);
  const [sending, setSending] = useState(false);

  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [activeStep, setActiveStep] = useState(1);
  const [showLargeAmountPopup, setShowLargeAmountPopup] = useState(false);
  const [showConfirmPopup, setShowConfirmPopup] = useState(false);
  const [showSuccessPopup, setShowSuccessPopup] = useState(false);

  const senderAccount = useMemo(
    () => accounts.find((account) => account.accountNumber === selectedSenderAccountNumber) || null,
    [accounts, selectedSenderAccountNumber],
  );

  const hasSender = Boolean(senderAccount);
  const hasReceiver = Boolean(receiver);
  const numericAmount = parseAmount(amount);
  const canPreview = hasSender && hasReceiver && Number.isFinite(numericAmount) && numericAmount > 0;
  const canEnterConfirmation = Boolean(preview);
  const canSend = canEnterConfirmation && isValidUpiPin(upiPin) && !sending;
  const stepItems = [
    { step: 1, label: 'Select Sender' },
    { step: 2, label: 'Enter Receiver' },
    { step: 3, label: 'Preview Charges' },
    { step: 4, label: 'Confirm PIN' },
    { step: 5, label: 'Transaction Result' },
  ];

  useEffect(() => {
    let active = true;

    async function loadCustomers() {
      setLoadingCustomers(true);

      try {
        const response = await getCustomers();
        if (active) {
          setCustomers(response);
        }
      } catch (error) {
        if (active) {
          setErrorMessage(error.message || 'Unable to load customers.');
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

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, [activeStep]);

  useEffect(() => {
    if (activeStep !== 2 || !hasSender) {
      return;
    }

    const trimmedReceiverAccount = receiverAccountNumber.trim();

    if (!trimmedReceiverAccount) {
      setReceiver(null);
      return;
    }

    if (trimmedReceiverAccount === selectedSenderAccountNumber) {
      setReceiver(null);
      return;
    }

    if (trimmedReceiverAccount.length < 10) {
      setReceiver(null);
      return;
    }

    let active = true;
    const timer = setTimeout(async () => {
      setLoadingReceiver(true);

      try {
        const receiverDetails = await getAccountByNumber(trimmedReceiverAccount);
        if (!active) {
          return;
        }
        setReceiver(receiverDetails);
        setErrorMessage('');
      } catch (error) {
        if (!active) {
          return;
        }
        setReceiver(null);
        setErrorMessage(error.message || 'Unable to fetch receiver details.');
      } finally {
        if (active) {
          setLoadingReceiver(false);
        }
      }
    }, 350);

    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [activeStep, hasSender, receiverAccountNumber, selectedSenderAccountNumber]);

  function resetPreviewAndResult() {
    setPreview(null);
    setResult(null);
    setSuccessMessage('');
  }

  async function handleSelectCustomer(customerId) {
    setSelectedCustomerId(customerId);
    setSelectedSenderAccountNumber('');
    setAccounts([]);
    setReceiverAccountNumber('');
    setReceiver(null);
    setAmount('');
    setUpiPin('');
    setDescription('');
    resetPreviewAndResult();
    setErrorMessage('');
    setActiveStep(1);

    if (!customerId) {
      return;
    }

    setLoadingAccounts(true);
    try {
      const customerAccounts = await getCustomerAccounts(customerId);
      setAccounts(customerAccounts);
      if (customerAccounts.length > 0) {
        setSelectedSenderAccountNumber(customerAccounts[0].accountNumber);
      }
    } catch (error) {
      setErrorMessage(error.message || 'Unable to fetch sender accounts.');
    } finally {
      setLoadingAccounts(false);
    }
  }

  function handleSelectSenderAccount(accountNumber) {
    setSelectedSenderAccountNumber(accountNumber);
    setReceiverAccountNumber('');
    setReceiver(null);
    setAmount('');
    setUpiPin('');
    setDescription('');
    resetPreviewAndResult();
    setErrorMessage('');
    setActiveStep(1);
  }

  function handleReceiverAccountChange(value) {
    setReceiverAccountNumber(value);
    setReceiver(null);
    setUpiPin('');
    resetPreviewAndResult();
    setErrorMessage('');
    setActiveStep(2);
  }

  async function handleLookupReceiver() {
    if (!hasSender) {
      setErrorMessage('Select sender account before fetching receiver details.');
      return false;
    }

    const trimmedReceiverAccount = receiverAccountNumber.trim();

    if (!trimmedReceiverAccount) {
      setErrorMessage('Receiver account number is required.');
      return false;
    }

    if (trimmedReceiverAccount === selectedSenderAccountNumber) {
      setErrorMessage('Sender and receiver account numbers cannot be the same.');
      return false;
    }

    setLoadingReceiver(true);
    setErrorMessage('');

    try {
      const receiverDetails = await getAccountByNumber(trimmedReceiverAccount);
      setReceiver(receiverDetails);
      return true;
    } catch (error) {
      setReceiver(null);
      setErrorMessage(error.message || 'Unable to fetch receiver details.');
      return false;
    } finally {
      setLoadingReceiver(false);
    }
  }

  function handleAmountChange(value) {
    setAmount(value);
    setUpiPin('');
    resetPreviewAndResult();
    setErrorMessage('');
    setActiveStep(3);
  }

  async function handlePreviewPayment() {
    if (!canPreview) {
      setErrorMessage('Select sender, fetch receiver, and enter a valid amount to preview payment.');
      return;
    }

    const previewPayload = {
      senderAccountNumber: selectedSenderAccountNumber,
      receiverAccountNumber: receiver.accountNumber,
      amount: numericAmount,
    };

    setPreviewing(true);
    setErrorMessage('');

    try {
      const previewResponse = await previewPayment(previewPayload);
      setPreview(previewResponse);
      setSuccessMessage('Payment preview generated successfully.');
      return true;
    } catch (error) {
      setPreview(null);
      setErrorMessage(error.message || 'Unable to preview payment.');
      return false;
    } finally {
      setPreviewing(false);
    }
  }

  async function executeSendPayment() {
    if (!preview) {
      setErrorMessage('Generate payment preview before sending.');
      return;
    }

    if (!isValidUpiPin(upiPin)) {
      setErrorMessage('UPI PIN must contain exactly 4 digits.');
      return;
    }

    if (!senderAccount || !receiver) {
      setErrorMessage('Sender and receiver details are required.');
      return;
    }

    const payload = {
      senderAccountNumber: senderAccount.accountNumber,
      receiverAccountNumber: receiver.accountNumber,
      receiverIfscCode: receiver.ifscCode,
      amount: numericAmount,
      upiPin,
      ...(description.trim() ? { description: description.trim() } : {}),
    };

    setSending(true);
    setErrorMessage('');
    setShowConfirmPopup(false);
    setShowLargeAmountPopup(false);

    try {
      const paymentResponse = await sendPayment(payload);
      setResult(paymentResponse);
      setSuccessMessage(paymentResponse.message || 'Payment completed successfully.');
      setUpiPin('');
      setActiveStep(5);
      setShowSuccessPopup(true);
    } catch (error) {
      setErrorMessage(error.message || 'Unable to send payment.');
    } finally {
      setSending(false);
    }
  }

  function handleSendPayment() {
    if (!preview) {
      setErrorMessage('Generate payment preview before sending.');
      return;
    }

    if (!isValidUpiPin(upiPin)) {
      setErrorMessage('UPI PIN must contain exactly 4 digits.');
      return;
    }

    if (numericAmount > 10000) {
      setShowLargeAmountPopup(true);
      return;
    }

    setShowConfirmPopup(true);
  }

  function handleCreateAnotherPayment() {
    setReceiverAccountNumber('');
    setReceiver(null);
    setAmount('');
    setDescription('');
    setUpiPin('');
    setPreview(null);
    setResult(null);
    setErrorMessage('');
    setSuccessMessage('Ready to create another payment.');
    setActiveStep(hasSender ? 2 : 1);
    setShowLargeAmountPopup(false);
    setShowConfirmPopup(false);
    setShowSuccessPopup(false);
  }

  function goBack() {
    setErrorMessage('');
    setSuccessMessage('');
    setShowLargeAmountPopup(false);
    setShowConfirmPopup(false);
    setActiveStep((step) => Math.max(1, step - 1));
  }

  function closeAllPopups() {
    setShowLargeAmountPopup(false);
    setShowConfirmPopup(false);
    setShowSuccessPopup(false);
  }

  function goNextFromStep1() {
    if (!hasSender) {
      setErrorMessage('Please select a customer and sender account to continue.');
      return;
    }
    setErrorMessage('');
    setActiveStep(2);
  }

  async function goNextFromStep2() {
    if (receiver && receiver.accountNumber === receiverAccountNumber.trim()) {
      setActiveStep(3);
      return;
    }

    const resolved = await handleLookupReceiver();
    if (resolved) {
      setActiveStep(3);
    }
  }

  function goNextFromStep3() {
    if (!preview) {
      setErrorMessage('Generate payment preview before continuing.');
      return;
    }
    setErrorMessage('');
    setActiveStep(4);
  }

  return (
    <div className="page-stack payment-page payment-page--wizard">
      <PageHeader
        eyebrow="Money transfer"
        title="Payments"
        description="Simple, secure transfer flow: select sender, verify receiver, preview charges, and confirm with UPI PIN."
      />

      <div className="payment-steps" aria-label="Payment progress">
        {stepItems.map((item) => {
          const state = item.step < activeStep ? 'done' : item.step === activeStep ? 'active' : 'upcoming';
          return (
            <div key={item.step} className={`payment-step payment-step--${state}`}>
              <span className="payment-step-index">{item.step}</span>
              <span>{item.label}</span>
            </div>
          );
        })}
      </div>

      <ErrorAlert message={errorMessage} onDismiss={() => setErrorMessage('')} />
      <SuccessAlert message={successMessage} onDismiss={() => setSuccessMessage('')} />

      {loadingCustomers ? <LoadingState label="Loading customers..." /> : null}

      {!loadingCustomers ? (
        <div className="payment-wizard-shell">
          {activeStep === 1 ? (
            <div className="payment-wizard-step">
              <CustomerSelector
                customers={customers}
                selectedCustomerId={selectedCustomerId}
                onSelectCustomer={handleSelectCustomer}
                loading={loadingAccounts}
              />
              {loadingAccounts ? (
                <LoadingState label="Loading sender accounts..." />
              ) : (
                <SenderAccountDetails
                  accounts={accounts}
                  selectedAccountNumber={selectedSenderAccountNumber}
                  onSelectAccount={handleSelectSenderAccount}
                />
              )}
              <div className="button-row payment-wizard-actions">
                <button
                  type="button"
                  className="primary-button"
                  onClick={goNextFromStep1}
                  disabled={!hasSender || loadingAccounts}
                >
                  Next
                </button>
              </div>
            </div>
          ) : null}

          {activeStep === 2 ? (
            <div className="payment-wizard-step">
              <ReceiverDetails
                receiverAccountNumber={receiverAccountNumber}
                onReceiverAccountNumberChange={handleReceiverAccountChange}
                onLookupReceiver={handleLookupReceiver}
                receiver={receiver}
                loading={loadingReceiver}
                disabled={!hasSender}
                showLookupButton={false}
              />
              <div className="button-row payment-wizard-actions">
                <button type="button" className="ghost-button" onClick={goBack}>
                  Back
                </button>
                <button
                  type="button"
                  className="primary-button"
                  onClick={goNextFromStep2}
                  disabled={!receiverAccountNumber.trim() || loadingReceiver}
                >
                  {loadingReceiver ? 'Verifying...' : 'Next'}
                </button>
              </div>
            </div>
          ) : null}

          {activeStep === 3 ? (
            <div className="payment-wizard-step">
              <PaymentPreview
                amount={amount}
                onAmountChange={handleAmountChange}
                onPreview={handlePreviewPayment}
                preview={preview}
                isPreviewing={previewing}
                senderCurrency={senderAccount?.currency}
                receiverCurrency={receiver?.currency}
                disabled={!hasSender || !hasReceiver}
              />
              <div className="button-row payment-wizard-actions">
                <button type="button" className="ghost-button" onClick={goBack}>
                  Back
                </button>
                <button
                  type="button"
                  className="primary-button"
                  onClick={goNextFromStep3}
                  disabled={!preview}
                >
                  Next
                </button>
              </div>
            </div>
          ) : null}

          {activeStep === 4 ? (
            <div className="payment-wizard-step">
              <PaymentConfirmation
                upiPin={upiPin}
                onUpiPinChange={setUpiPin}
                description={description}
                onDescriptionChange={setDescription}
                onSend={handleSendPayment}
                sending={sending}
                disabled={!canEnterConfirmation}
                canSubmit={canSend}
              />
              <div className="button-row payment-wizard-actions">
                <button type="button" className="ghost-button" onClick={goBack} disabled={sending}>
                  Back
                </button>
              </div>
            </div>
          ) : null}

          {activeStep === 5 ? (
            <div className="payment-wizard-step">
              <PaymentSuccess result={result} onCreateAnother={handleCreateAnotherPayment} />
            </div>
          ) : null}
        </div>
      ) : null}

      {showLargeAmountPopup ? (
        <div className="modal-overlay" role="dialog" aria-modal="true" aria-label="Large transaction warning">
          <div className="modal-card">
            <h3>Large transaction alert</h3>
            <p>
              You are sending an amount greater than 10000. Do you want to continue with this high-value payment?
            </p>
            <div className="button-row modal-actions">
              <button type="button" className="ghost-button" onClick={closeAllPopups} disabled={sending}>
                Cancel
              </button>
              <button
                type="button"
                className="primary-button"
                onClick={() => {
                  setShowLargeAmountPopup(false);
                  setShowConfirmPopup(true);
                }}
                disabled={sending}
              >
                Continue
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {showConfirmPopup ? (
        <div className="modal-overlay" role="dialog" aria-modal="true" aria-label="Confirm payment">
          <div className="modal-card">
            <h3>Confirm payment</h3>
            <p>Please confirm if you want to proceed with this payment.</p>
            <div className="modal-summary">
              <span>Sender</span>
              <strong>{senderAccount?.accountNumber}</strong>
              <span>Receiver</span>
              <strong>{receiver?.accountNumber}</strong>
              <span>Amount</span>
              <strong>{amount}</strong>
            </div>
            <div className="button-row modal-actions">
              <button type="button" className="ghost-button" onClick={closeAllPopups} disabled={sending}>
                Cancel
              </button>
              <button type="button" className="primary-button" onClick={executeSendPayment} disabled={sending}>
                {sending ? 'Processing...' : 'Confirm'}
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {showSuccessPopup && result ? (
        <div className="modal-overlay" role="dialog" aria-modal="true" aria-label="Payment successful">
          <div className="modal-card modal-card--success">
            <div className="success-icon" aria-hidden="true">
              <span>OK</span>
            </div>
            <h3>Payment successful</h3>
            <p>Your transfer is completed.</p>
            <div className="modal-summary">
              <span>Transaction ID</span>
              <strong>{result.transactionId}</strong>
              <span>Status</span>
              <strong>{result.paymentStatus}</strong>
            </div>
            <div className="button-row modal-actions">
              <button type="button" className="primary-button" onClick={() => setShowSuccessPopup(false)}>
                Done
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

