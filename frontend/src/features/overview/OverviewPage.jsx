import { useEffect, useMemo, useState } from 'react';
import { getAllCustomers } from '../../api/customerApi';
import { getAllTransactions } from '../../api/paymentApi';
import { getSupportDashboard } from '../../api/supportApi';
import {
  DataTable,
  EmptyState,
  ErrorAlert,
  LoadingState,
  PageHeader,
  SectionCard,
  StatusBadge,
  StatCard,
} from '../../components/UI';
import {
  formatCurrency,
  formatDateTime,
  getEffectiveTransactionTime,
} from '../../utils/formatters';

export function OverviewPage() {
  const [dashboard, setDashboard] = useState(null);
  const [customers, setCustomers] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;

    async function loadOverview() {
      setLoading(true);
      setError('');

      try {
        const [dashboardData, customerData, transactionData] = await Promise.all([
          getSupportDashboard(),
          getAllCustomers(),
          getAllTransactions(),
        ]);

        if (!active) {
          return;
        }

        setDashboard(dashboardData);
        setCustomers(customerData);
        setTransactions(transactionData);
      } catch (err) {
        if (active) {
          setError(err.message || 'Unable to load the overview.');
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadOverview();
    return () => {
      active = false;
    };
  }, []);

  const recentTransactions = useMemo(() => {
    return [...transactions]
      .sort((left, right) => {
        const leftTime = new Date(getEffectiveTransactionTime(left) || 0).getTime();
        const rightTime = new Date(getEffectiveTransactionTime(right) || 0).getTime();
        return rightTime - leftTime;
      })
      .slice(0, 5);
  }, [transactions]);

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Operational dashboard"
        title="Overview"
        description="Monitor the payment system, inspect recent activity, and use the seeded customer accounts for quick demos."
      />

      <ErrorAlert message={error} onDismiss={() => setError('')} />

      {loading ? (
        <LoadingState label="Loading overview metrics and recent activity..." />
      ) : (
        <>
          <div className="stats-grid">
            <StatCard label="Total customers" value={dashboard?.totalCustomers ?? customers.length} />
            <StatCard label="Total transactions" value={dashboard?.totalTransactions ?? transactions.length} />
            <StatCard
              label="Successful transactions"
              value={dashboard?.successfulTransactions ?? '—'}
              tone="success"
            />
            <StatCard
              label="Failed transactions"
              value={dashboard?.failedTransactions ?? '—'}
              tone="danger"
            />
            <StatCard
              label="Total credit"
              value={formatCurrency(dashboard?.totalCreditAmount)}
              tone="info"
            />
            <StatCard
              label="Total debit"
              value={formatCurrency(dashboard?.totalDebitAmount)}
              tone="warning"
            />
          </div>

          <div className="content-grid content-grid--2col">
            <SectionCard
              title="Quick start"
              subtitle="The backend ships with sample records, so you can test flows immediately."
            >
              <ul className="bullet-list">
                <li>Alice Johnson — account <strong>100000000001</strong>, IFSC <strong>SBIN0001234</strong></li>
                <li>Bob Smith — account <strong>100000000002</strong>, IFSC <strong>HDFC0005678</strong></li>
                <li>Try a payment from Alice to Bob using UPI PIN <strong>1234</strong></li>
                <li>Use the Transactions page to retry the seeded failed transaction</li>
              </ul>
            </SectionCard>

            <SectionCard title="Customers in system" subtitle="Fetched from `/api/customers`.">
              {customers.length ? (
                <div className="list-stack">
                  {customers.map((customer) => (
                    <div key={customer.customerId} className="inline-card">
                      <div>
                        <strong>{customer.customerName}</strong>
                        <p>{customer.bankName}</p>
                      </div>
                      <div className="text-right">
                        <span>{customer.accountNumber}</span>
                        <p>{formatCurrency(customer.balance)}</p>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <EmptyState title="No customers returned" description="Check backend seed data or MySQL startup." />
              )}
            </SectionCard>
          </div>

          <SectionCard title="Recent transactions" subtitle="Latest payment activity from `/api/payments`.">
            <DataTable
              rowKey={(transaction) => transaction.transactionId}
              rows={recentTransactions}
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
                  key: 'createdTime',
                  label: 'Latest time',
                  render: (transaction) => formatDateTime(getEffectiveTransactionTime(transaction)),
                },
                {
                  key: 'paymentStatus',
                  label: 'Status',
                  render: (transaction) => <StatusBadge status={transaction.paymentStatus} />,
                },
              ]}
              empty={<EmptyState title="No transactions yet" description="Create or retry a payment to populate the table." />}
            />
          </SectionCard>
        </>
      )}
    </div>
  );
}


