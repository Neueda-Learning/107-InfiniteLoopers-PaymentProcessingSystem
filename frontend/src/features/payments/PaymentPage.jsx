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
  }

  function handleReceiverAccountChange(value) {
    setReceiverAccountNumber(value);
    setReceiver(null);
    setUpiPin('');
    resetPreviewAndResult();
    setErrorMessage('');
  }

  async function handleLookupReceiver() {
    if (!hasSender) {
      setErrorMessage('Select sender account before fetching receiver details.');
      return;
    }

    const trimmedReceiverAccount = receiverAccountNumber.trim();

    if (!trimmedReceiverAccount) {
      setErrorMessage('Receiver account number is required.');
      return;
    }

    if (trimmedReceiverAccount === selectedSenderAccountNumber) {
      setErrorMessage('Sender and receiver account numbers cannot be the same.');
      return;
    }

    setLoadingReceiver(true);
    setErrorMessage('');

    try {
      const receiverDetails = await getAccountByNumber(trimmedReceiverAccount);
      setReceiver(receiverDetails);
    } catch (error) {
      setReceiver(null);
      setErrorMessage(error.message || 'Unable to fetch receiver details.');
    } finally {
      setLoadingReceiver(false);
    }
  }

  function handleAmountChange(value) {
    setAmount(value);
    setUpiPin('');
    resetPreviewAndResult();
    setErrorMessage('');
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
    } catch (error) {
      setPreview(null);
      setErrorMessage(error.message || 'Unable to preview payment.');
    } finally {
      setPreviewing(false);
    }
  }

  async function handleSendPayment() {
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

    try {
      const paymentResponse = await sendPayment(payload);
      setResult(paymentResponse);
      setSuccessMessage(paymentResponse.message || 'Payment completed successfully.');
      setUpiPin('');
    } catch (error) {
      setErrorMessage(error.message || 'Unable to send payment.');
    } finally {
      setSending(false);
    }
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
  }

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Money transfer"
        title="Payments"
        description="Complete payment flow aligned with backend APIs: select sender, fetch receiver, preview charges, confirm with UPI PIN, and submit transaction."
      />

      <ErrorAlert message={errorMessage} onDismiss={() => setErrorMessage('')} />
      <SuccessAlert message={successMessage} onDismiss={() => setSuccessMessage('')} />

      {loadingCustomers ? <LoadingState label="Loading customers..." /> : null}

      {!loadingCustomers ? (
        <div className="payment-flow-grid">
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

          <ReceiverDetails
            receiverAccountNumber={receiverAccountNumber}
            onReceiverAccountNumberChange={handleReceiverAccountChange}
            onLookupReceiver={handleLookupReceiver}
            receiver={receiver}
            loading={loadingReceiver}
            disabled={!hasSender}
          />

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

          <PaymentSuccess result={result} onCreateAnother={handleCreateAnotherPayment} />
        </div>
      ) : null}
    </div>
  );
}

