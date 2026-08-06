import { useEffect, useMemo, useState } from 'react';
import { getAllTransactions, getTransactionById, retryPayment } from '../../api/paymentApi';
import { getAllCustomers } from '../../api/customerApi';
import {
  DataTable,
  EmptyState,
  ErrorAlert,
  LoadingState,
  PageHeader,
  SectionCard,
  StatusBadge,
  SuccessAlert,
} from '../../components/UI';
import {
  buildTransactionTimeline,
  formatCurrency,
  formatDateTime,
  getEffectiveTransactionTime,
} from '../../utils/formatters';

export function TransactionsPage() {
  const [transactions, setTransactions] = useState([]);
  const [customerAccountById, setCustomerAccountById] = useState({});
  const [selectedTransaction, setSelectedTransaction] = useState(null);
  const [loading, setLoading] = useState(true);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [retryingId, setRetryingId] = useState('');
  const [customerSearchInput, setCustomerSearchInput] = useState('');
  const [appliedCustomerId, setAppliedCustomerId] = useState('');
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  async function loadTransactions(selectTransactionId) {
    setLoading(true);
    setError('');

    try {
      const [transactionData, customerData] = await Promise.all([
        getAllTransactions(),
        getAllCustomers(),
      ]);
      setTransactions(transactionData);
      setCustomerAccountById(
        customerData.reduce((acc, customer) => {
          acc[String(customer.customerId)] = customer.accountNumber;
          return acc;
        }, {}),
      );

      if (selectTransactionId) {
        const match = transactionData.find((transaction) => transaction.transactionId === selectTransactionId);
        if (match) {
          setSelectedTransaction(match);
        }
      } else if (!selectedTransaction && transactionData.length) {
        setSelectedTransaction(transactionData[0]);
      }
    } catch (err) {
      setError(err.message || 'Unable to load transactions.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadTransactions();
  }, []);

  const filteredTransactions = useMemo(() => {
    if (!appliedCustomerId.trim()) {
      return transactions;
    }

    const accountNumber = customerAccountById[appliedCustomerId.trim()];
    if (!accountNumber) {
      return [];
    }

    return transactions.filter(
      (transaction) =>
        transaction.senderAccountNumber === accountNumber
        || transaction.receiverAccountNumber === accountNumber,
    );
  }, [transactions, appliedCustomerId, customerAccountById]);

  function handleCustomerSearch(event) {
    event.preventDefault();
    setAppliedCustomerId(customerSearchInput.trim());
  }

  function handleClearCustomerSearch() {
    setCustomerSearchInput('');
    setAppliedCustomerId('');
  }

  async function handleSelectTransaction(transactionId) {
    setDetailsLoading(true);
    setError('');

    try {
      const data = await getTransactionById(transactionId);
      setSelectedTransaction(data);
    } catch (err) {
      setError(err.message || 'Unable to load transaction details.');
    } finally {
      setDetailsLoading(false);
    }
  }

  async function handleRetry(transactionId) {
    setRetryingId(transactionId);
    setError('');
    setSuccessMessage('');

    try {
      const response = await retryPayment(transactionId);
      setSuccessMessage(response.message || 'Retry submitted successfully.');
      await loadTransactions(response.transactionId);
      await handleSelectTransaction(response.transactionId);
    } catch (err) {
      setError(err.message || 'Unable to retry this transaction.');
    } finally {
      setRetryingId('');
    }
  }

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Transaction operations"
        title="Transactions"
        description="Inspect every transaction, fetch full details, and retry failed payments according to backend rules."
      />

      <ErrorAlert message={error} onDismiss={() => setError('')} />
      <SuccessAlert message={successMessage} onDismiss={() => setSuccessMessage('')} />

      <SectionCard title="Search transactions">
        <form className="filter-row" onSubmit={handleCustomerSearch}>
          <input
            value={customerSearchInput}
            onChange={(event) => setCustomerSearchInput(event.target.value)}
            placeholder="Search by customer ID"
          />
          <button type="submit" className="primary-button">Search</button>
          <button type="button" className="ghost-button" onClick={handleClearCustomerSearch}>Clear</button>
          <button type="button" className="secondary-button" onClick={() => loadTransactions()}>Refresh</button>
        </form>
      </SectionCard>

      <div className="content-grid content-grid--2col">
        <SectionCard title="All transactions" subtitle="Click a row to load `GET /api/payments/{transactionId}` details.">
          {loading ? (
            <LoadingState label="Loading transactions..." />
          ) : (
            <DataTable
              rowKey={(transaction) => transaction.transactionId}
              rows={filteredTransactions}
              onRowClick={(transaction) => handleSelectTransaction(transaction.transactionId)}
              columns={[
                { key: 'transactionId', label: 'Transaction ID' },
                {
                  key: 'amount',
                  label: 'Amount',
                  render: (transaction) => formatCurrency(transaction.amount),
                },
                { key: 'senderAccountNumber', label: 'Sender' },
                { key: 'receiverAccountNumber', label: 'Receiver' },
                {
                  key: 'paymentStatus',
                  label: 'Status',
                  render: (transaction) => <StatusBadge status={transaction.paymentStatus} />,
                },
                {
                  key: 'createdTime',
                  label: 'Latest time',
                  render: (transaction) => formatDateTime(getEffectiveTransactionTime(transaction)),
                },
              ]}
              empty={
                <EmptyState
                  title="No transactions match your filters"
                  description="Try clearing the search text or selecting a different status."
                />
              }
            />
          )}
        </SectionCard>

        <SectionCard title="Transaction details" subtitle="Useful for tracing status changes and retry eligibility.">
          {detailsLoading ? (
            <LoadingState label="Loading selected transaction..." />
          ) : selectedTransaction ? (
            <div className="detail-stack">
              <div className="detail-grid">
                <div>
                  <span className="detail-label">Transaction ID</span>
                  <strong>{selectedTransaction.transactionId}</strong>
                </div>
                <div>
                  <span className="detail-label">Status</span>
                  <StatusBadge status={selectedTransaction.paymentStatus} />
                </div>
                <div>
                  <span className="detail-label">Sender</span>
                  <strong>{selectedTransaction.senderAccountNumber}</strong>
                </div>
                <div>
                  <span className="detail-label">Receiver</span>
                  <strong>{selectedTransaction.receiverAccountNumber}</strong>
                </div>
                <div>
                  <span className="detail-label">Amount</span>
                  <strong>{formatCurrency(selectedTransaction.amount)}</strong>
                </div>
                <div>
                  <span className="detail-label">Description</span>
                  <strong>{selectedTransaction.description || '—'}</strong>
                </div>
              </div>

              <div>
                <h3 className="subheading">Timeline</h3>
                <div className="timeline">
                  {buildTransactionTimeline(selectedTransaction).map(([label, timestamp]) => (
                    <div key={label} className="timeline-item">
                      <div className="timeline-dot" />
                      <div>
                        <strong>{label}</strong>
                        <p>{formatDateTime(timestamp)}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {selectedTransaction.paymentStatus === 'FAILED' ? (
                <button
                  type="button"
                  className="primary-button"
                  onClick={() => handleRetry(selectedTransaction.transactionId)}
                  disabled={retryingId === selectedTransaction.transactionId}
                >
                  {retryingId === selectedTransaction.transactionId ? 'Retrying...' : 'Retry failed payment'}
                </button>
              ) : (
                <p className="muted">Only transactions in `FAILED` status can be retried.</p>
              )}
            </div>
          ) : (
            <EmptyState title="No transaction selected" description="Select a transaction row to inspect its timeline." />
          )}
        </SectionCard>
      </div>
    </div>
  );
}

