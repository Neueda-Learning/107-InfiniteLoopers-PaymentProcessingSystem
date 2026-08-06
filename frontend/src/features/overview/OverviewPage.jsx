import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { getAllCustomers } from '../../api/customerApi';
import { getAllTransactions } from '../../api/paymentApi';
import { getSupportDashboard } from '../../api/supportApi';
import {
  ErrorAlert,
  LoadingState,
  PageHeader,
  StatCard,
} from '../../components/UI';
import {
  formatCurrency,
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

  const overviewMetrics = useMemo(() => {
    const totalCustomers = Number(dashboard?.totalCustomers ?? customers.length ?? 0);
    const totalTransactions = Number(dashboard?.totalTransactions ?? transactions.length ?? 0);
    const successfulTransactions = Number(
      dashboard?.successfulTransactions
        ?? transactions.filter((item) => item.paymentStatus === 'COMPLETED').length,
    );
    const failedTransactions = Number(
      dashboard?.failedTransactions
        ?? transactions.filter((item) => item.paymentStatus === 'FAILED').length,
    );

    const averageAmount = transactions.length
      ? transactions.reduce((sum, item) => sum + Number(item.amount || 0), 0) / transactions.length
      : 0;

    return {
      totalCustomers,
      totalTransactions,
      successfulTransactions,
      failedTransactions,
      averageAmount,
    };
  }, [customers, dashboard, transactions]);

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Dashboard"
        title="Overview"
        description="A summary of your payment activity. See how your transactions are performing and take action from here."
      />

      <ErrorAlert message={error} onDismiss={() => setError('')} />

      {loading ? (
        <LoadingState label="Loading your dashboard..." />
      ) : (
        <>
          <section className="overview-hero" aria-label="Overview hero">
            <div>
              <p className="overview-hero__eyebrow">Welcome to PayPilot</p>
              <h2>Your payments, all in one place.</h2>
              <p className="muted">
                Send payments, check transaction status, and get support — all from this dashboard.
              </p>
              <div className="overview-hero__actions">
                <Link to="/payments" className="primary-button">Make a Payment</Link>
                <Link to="/support" className="secondary-button">View Support</Link>
              </div>
            </div>
          </section>

          <div className="stats-grid">
            <StatCard label="Registered Customers" value={overviewMetrics.totalCustomers} />
            <StatCard label="Total Transactions" value={overviewMetrics.totalTransactions} />
            <StatCard
              label="Successful Transactions"
              value={overviewMetrics.successfulTransactions}
              tone="success"
            />
            <StatCard
              label="Average Transfer Amount"
              value={formatCurrency(overviewMetrics.averageAmount)}
            />
            <StatCard
              label="Failed Transactions"
              value={overviewMetrics.failedTransactions}
              tone="danger"
            />
          </div>

        </>
      )}
    </div>
  );
}
