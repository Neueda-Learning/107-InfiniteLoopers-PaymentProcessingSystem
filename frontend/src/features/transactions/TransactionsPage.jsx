import { useEffect, useMemo, useState } from 'react';
import { getAllTransactions, getTransactionById, retryPayment } from '../../api/paymentApi';
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
  PAYMENT_STATUSES,
  buildTransactionTimeline,
  formatCurrency,
  formatDateTime,
  getEffectiveTransactionTime,
} from '../../utils/formatters';

export function TransactionsPage() {
  const [transactions, setTransactions] = useState([]);
  const [selectedTransaction, setSelectedTransaction] = useState(null);
  const [loading, setLoading] = useState(true);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [retryingId, setRetryingId] = useState('');
  const [query, setQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  async function loadTransactions(selectTransactionId) {
    setLoading(true);
    setError('');

    try {
      const data = await getAllTransactions();
      setTransactions(data);

      if (selectTransactionId) {
        const match = data.find((transaction) => transaction.transactionId === selectTransactionId);
        if (match) {
          setSelectedTransaction(match);
        }
      } else if (!selectedTransaction && data.length) {
        setSelectedTransaction(data[0]);
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
    return transactions.filter((transaction) => {
      const matchesStatus = statusFilter === 'ALL' || transaction.paymentStatus === statusFilter;
      const searchable = [
        transaction.transactionId,
        transaction.senderAccountNumber,
        transaction.receiverAccountNumber,
        transaction.description,
      ]
        .join(' ')
        .toLowerCase();
      const matchesQuery = searchable.includes(query.trim().toLowerCase());
      return matchesStatus && matchesQuery;
    });
  }, [transactions, query, statusFilter]);

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

      <SectionCard title="Search and filter" subtitle="Client-side filters layered on top of `GET /api/payments`.">
        <div className="filter-row">
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search by transaction ID, sender, receiver, or description"
          />
          <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
            <option value="ALL">All statuses</option>
            {PAYMENT_STATUSES.map((status) => (
              <option key={status} value={status}>
                {status}
              </option>
            ))}
          </select>
          <button type="button" className="secondary-button" onClick={() => loadTransactions()}>
            Refresh
          </button>
        </div>
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

