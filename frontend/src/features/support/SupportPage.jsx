import { useEffect, useState } from 'react';
import {
  getSupportDashboard,
  getSupportTransactions,
  getSupportTransactionsByCustomer,
  getSupportTransactionsByStatus,
} from '../../api/supportApi';
import {
  DataTable,
  EmptyState,
  ErrorAlert,
  LoadingState,
  PageHeader,
  SectionCard,
  StatCard,
  StatusBadge,
} from '../../components/UI';
import {
  PAYMENT_STATUSES,
  formatCurrency,
  formatDateTime,
  getEffectiveTransactionTime,
} from '../../utils/formatters';

export function SupportPage() {
  const [dashboard, setDashboard] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [filterLoading, setFilterLoading] = useState(false);
  const [error, setError] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [accountFilter, setAccountFilter] = useState('');

  async function loadDashboard() {
    const [dashboardData, transactionData] = await Promise.all([
      getSupportDashboard(),
      getSupportTransactions(),
    ]);

    setDashboard(dashboardData);
    setTransactions(transactionData);
  }

  useEffect(() => {
    let active = true;

    async function initialize() {
      setLoading(true);
      setError('');

      try {
        await loadDashboard();
      } catch (err) {
        if (active) {
          setError(err.message || 'Unable to load support dashboard.');
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    initialize();
    return () => {
      active = false;
    };
  }, []);

  async function refreshDashboard() {
    setRefreshing(true);
    setError('');

    try {
      await loadDashboard();
    } catch (err) {
      setError(err.message || 'Unable to refresh support dashboard.');
    } finally {
      setRefreshing(false);
    }
  }

  async function applyFilters() {
    setFilterLoading(true);
    setError('');

    try {
      if (accountFilter.trim()) {
        const data = await getSupportTransactionsByCustomer(accountFilter.trim());
        setTransactions(data);
      } else if (statusFilter !== 'ALL') {
        const data = await getSupportTransactionsByStatus(statusFilter);
        setTransactions(data);
      } else {
        const data = await getSupportTransactions();
        setTransactions(data);
      }
    } catch (err) {
      setError(err.message || 'Unable to apply support filters.');
    } finally {
      setFilterLoading(false);
    }
  }

  async function resetFilters() {
    setStatusFilter('ALL');
    setAccountFilter('');
    setFilterLoading(true);
    setError('');

    try {
      const data = await getSupportTransactions();
      setTransactions(data);
    } catch (err) {
      setError(err.message || 'Unable to reset filters.');
    } finally {
      setFilterLoading(false);
    }
  }

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Support operations"
        title="Support dashboard"
        description="Track platform health metrics and filter transactions by status or customer account using support-specific endpoints."
        actions={
          <button type="button" className="secondary-button" onClick={refreshDashboard} disabled={refreshing}>
            {refreshing ? 'Refreshing...' : 'Refresh metrics'}
          </button>
        }
      />

      <ErrorAlert message={error} onDismiss={() => setError('')} />

      {loading ? (
        <LoadingState label="Loading dashboard metrics..." />
      ) : (
        <>
          <div className="stats-grid">
            <StatCard label="Total customers" value={dashboard?.totalCustomers ?? '—'} />
            <StatCard label="Total transactions" value={dashboard?.totalTransactions ?? '—'} />
            <StatCard label="Successful" value={dashboard?.successfulTransactions ?? '—'} tone="success" />
            <StatCard label="Failed" value={dashboard?.failedTransactions ?? '—'} tone="danger" />
            <StatCard label="Total credit" value={formatCurrency(dashboard?.totalCreditAmount)} tone="info" />
            <StatCard label="Total debit" value={formatCurrency(dashboard?.totalDebitAmount)} tone="warning" />
          </div>

          <SectionCard title="Operational filters" subtitle="Backend-driven filters using `GET /api/support/status/{status}` and `GET /api/support/customer/{accountNumber}`.">
            <div className="filter-row">
              <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
                <option value="ALL">All statuses</option>
                {PAYMENT_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {status}
                  </option>
                ))}
              </select>
              <input
                value={accountFilter}
                onChange={(event) => setAccountFilter(event.target.value)}
                placeholder="Filter by account number"
              />
              <button type="button" className="primary-button" onClick={applyFilters} disabled={filterLoading}>
                {filterLoading ? 'Applying...' : 'Apply filters'}
              </button>
              <button type="button" className="ghost-button" onClick={resetFilters} disabled={filterLoading}>
                Reset
              </button>
            </div>
            <p className="muted">Account filter takes priority over status filter when both are filled.</p>
          </SectionCard>

          <SectionCard title="Support transaction monitor" subtitle="System-wide transaction feed for operations and troubleshooting.">
            {filterLoading ? (
              <LoadingState label="Applying filters..." />
            ) : (
              <DataTable
                rowKey={(transaction) => transaction.transactionId}
                rows={transactions}
                columns={[
                  { key: 'transactionId', label: 'Transaction ID' },
                  { key: 'senderAccountNumber', label: 'Sender' },
                  { key: 'receiverAccountNumber', label: 'Receiver' },
                  {
                    key: 'amount',
                    label: 'Amount',
                    render: (transaction) => formatCurrency(transaction.amount),
                  },
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
                    title="No transactions returned"
                    description="Try resetting filters or verify data exists in the backend database."
                  />
                }
              />
            )}
          </SectionCard>
        </>
      )}
    </div>
  );
}


