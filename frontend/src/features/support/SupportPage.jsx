import { useEffect, useMemo, useState } from 'react';
import { getSupportTransactions, getSupportTransactionsByCustomer } from '../../api/supportApi';
import {
  getCustomerById,
  getCustomerAccounts,
  getCustomerTransactions,
  getCustomers,
} from '../../api/customerApi';
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
import { formatCurrency, formatDateTime, getEffectiveTransactionTime } from '../../utils/formatters';

const SUPPORT_TABS = [
  { key: 'transactions', label: 'Transactions' },
  { key: 'customers', label: 'Customers' },
  { key: 'audit', label: 'Audit Trail' },
];

const ALL_CUSTOMERS = 'ALL_CUSTOMERS';

function sortByLatestTime(left, right) {
  const leftTime = new Date(getEffectiveTransactionTime(left) || 0).getTime();
  const rightTime = new Date(getEffectiveTransactionTime(right) || 0).getTime();
  return rightTime - leftTime;
}

function mergeTransactions(transactions) {
  const byTransactionId = new Map();

  transactions.forEach((transaction) => {
    byTransactionId.set(transaction.transactionId, transaction);
  });

  return Array.from(byTransactionId.values());
}

function addCurrencyAmount(bucket, currency, amount) {
  const safeCurrency = currency || 'INR';
  const safeAmount = Number(amount || 0);
  bucket[safeCurrency] = (bucket[safeCurrency] || 0) + safeAmount;
}

function formatCurrencyBreakdown(totals) {
  const entries = Object.entries(totals)
    .filter(([, amount]) => Number(amount) !== 0)
    .sort(([left], [right]) => left.localeCompare(right));

  if (!entries.length) {
    return '—';
  }

  return entries
    .map(([currency, amount]) => formatCurrency(amount, currency))
    .join(' · ');
}

function getTransactionBaseCurrency(transaction) {
  return transaction.senderCurrency || transaction.receiverCurrency || 'INR';
}

export function SupportPage() {
  const [activeTab, setActiveTab] = useState('transactions');
  const [customerOptions, setCustomerOptions] = useState([]);
  const [allTransactions, setAllTransactions] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [transactionsLoading, setTransactionsLoading] = useState(false);
  const [customerInsightsLoading, setCustomerInsightsLoading] = useState(false);
  const [error, setError] = useState('');

  const [transactionCustomerFilter, setTransactionCustomerFilter] = useState(ALL_CUSTOMERS);
  const [selectedCustomerId, setSelectedCustomerId] = useState('');
  const [selectedCustomer, setSelectedCustomer] = useState(null);
  const [selectedCustomerAccounts, setSelectedCustomerAccounts] = useState([]);
  const [selectedCustomerTransactions, setSelectedCustomerTransactions] = useState([]);

  const selectedAccountNumbers = useMemo(
    () => new Set(selectedCustomerAccounts.map((account) => account.accountNumber)),
    [selectedCustomerAccounts],
  );

  const selectedAccountsByNumber = useMemo(
    () => selectedCustomerAccounts.reduce((accumulator, account) => {
      accumulator[account.accountNumber] = account;
      return accumulator;
    }, {}),
    [selectedCustomerAccounts],
  );

  const customerAnalytics = useMemo(() => {
    const totalBalanceByCurrency = {};
    const totalSentByCurrency = {};
    const totalReceivedByCurrency = {};

    selectedCustomerAccounts.forEach((account) => {
      addCurrencyAmount(totalBalanceByCurrency, account.currency, account.balance);
    });

    selectedCustomerTransactions.forEach((transaction) => {
      const senderAccount = selectedAccountsByNumber[transaction.senderAccountNumber];
      const receiverAccount = selectedAccountsByNumber[transaction.receiverAccountNumber];

      if (senderAccount) {
        addCurrencyAmount(totalSentByCurrency, senderAccount.currency, transaction.amount);
      }

      if (receiverAccount) {
        addCurrencyAmount(
          totalReceivedByCurrency,
          receiverAccount.currency,
          transaction.convertedAmount ?? transaction.amount,
        );
      }
    });

    const sentComparisonTotal = Object.values(totalSentByCurrency).reduce(
      (sum, amount) => sum + Number(amount || 0),
      0,
    );
    const receivedComparisonTotal = Object.values(totalReceivedByCurrency).reduce(
      (sum, amount) => sum + Number(amount || 0),
      0,
    );

    return {
      totalBalanceByCurrency,
      totalSentByCurrency,
      totalReceivedByCurrency,
      totalTransactionsCount: selectedCustomerTransactions.length,
      successfulCount: selectedCustomerTransactions.filter((item) => item.paymentStatus === 'COMPLETED').length,
      failedCount: selectedCustomerTransactions.filter((item) => item.paymentStatus === 'FAILED').length,
      sentComparisonTotal,
      receivedComparisonTotal,
    };
  }, [selectedAccountsByNumber, selectedCustomerAccounts, selectedCustomerTransactions]);

  useEffect(() => {
    let active = true;

    async function loadInitialData() {
      setLoading(true);
      setError('');

      try {
        const [customerData, transactionData] = await Promise.all([
          getCustomers(),
          getSupportTransactions(),
        ]);

        if (!active) {
          return;
        }

        const sortedTransactions = [...transactionData].sort(sortByLatestTime);
        setCustomerOptions(customerData);
        setAllTransactions(sortedTransactions);
        setTransactions(sortedTransactions);

        if (customerData.length) {
          setSelectedCustomerId(String(customerData[0].id));
        }
      } catch (err) {
        if (active) {
          setError(err.message || 'Unable to load support data.');
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadInitialData();

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (activeTab !== 'customers' || !selectedCustomerId) {
      return;
    }

    let active = true;

    async function loadCustomerInsights() {
      setCustomerInsightsLoading(true);
      setError('');
      setSelectedCustomer(null);
      setSelectedCustomerAccounts([]);
      setSelectedCustomerTransactions([]);

      try {
        const [customerDetails, accounts] = await Promise.all([
          getCustomerById(selectedCustomerId),
          getCustomerAccounts(selectedCustomerId),
        ]);

        if (!active) {
          return;
        }

        const transactionGroups = await Promise.all(
          accounts.map((account) => getCustomerTransactions(account.accountNumber)),
        );

        if (!active) {
          return;
        }

        setSelectedCustomer(customerDetails);
        setSelectedCustomerAccounts(accounts);
        setSelectedCustomerTransactions(mergeTransactions(transactionGroups.flat()).sort(sortByLatestTime));
      } catch (err) {
        if (active) {
          setError(err.message || 'Unable to load customer support insights.');
          setSelectedCustomer(null);
          setSelectedCustomerAccounts([]);
          setSelectedCustomerTransactions([]);
        }
      } finally {
        if (active) {
          setCustomerInsightsLoading(false);
        }
      }
    }

    loadCustomerInsights();

    return () => {
      active = false;
    };
  }, [activeTab, selectedCustomerId]);

  async function handleTransactionCustomerFilterChange(event) {
    const customerId = event.target.value;
    setTransactionCustomerFilter(customerId);
    setTransactionsLoading(true);
    setError('');

    try {
      if (customerId === ALL_CUSTOMERS) {
        setTransactions(allTransactions);
        return;
      }

      const accounts = await getCustomerAccounts(customerId);

      if (!accounts.length) {
        setTransactions([]);
        return;
      }

      const transactionGroups = await Promise.all(
        accounts.map((account) => getSupportTransactionsByCustomer(account.accountNumber)),
      );

      setTransactions(mergeTransactions(transactionGroups.flat()).sort(sortByLatestTime));
    } catch (err) {
      setError(err.message || 'Unable to apply transaction customer filter.');
    } finally {
      setTransactionsLoading(false);
    }
  }

  function renderDirectionBadge(transaction) {
    const isSent = selectedAccountNumbers.has(transaction.senderAccountNumber);
    const isReceived = selectedAccountNumbers.has(transaction.receiverAccountNumber);

    if (isSent && isReceived) {
      return <span className="badge badge--info">Internal</span>;
    }

    if (isSent) {
      return <span className="badge badge--warning">Sent</span>;
    }

    if (isReceived) {
      return <span className="badge badge--success">Received</span>;
    }

    return <span className="badge badge--neutral">Related</span>;
  }

  function getRelatedAccountNumber(transaction) {
    if (selectedAccountNumbers.has(transaction.senderAccountNumber)) {
      return transaction.senderAccountNumber;
    }

    if (selectedAccountNumbers.has(transaction.receiverAccountNumber)) {
      return transaction.receiverAccountNumber;
    }

    return '—';
  }

  function getTransactionDisplayAmount(transaction) {
    const receiverAccount = selectedAccountsByNumber[transaction.receiverAccountNumber];
    const senderAccount = selectedAccountsByNumber[transaction.senderAccountNumber];

    if (receiverAccount) {
      return formatCurrency(
        transaction.convertedAmount ?? transaction.amount,
        receiverAccount.currency || transaction.receiverCurrency || 'INR',
      );
    }

    if (senderAccount) {
      return formatCurrency(transaction.amount, senderAccount.currency || transaction.senderCurrency || 'INR');
    }

    return formatCurrency(transaction.amount, getTransactionBaseCurrency(transaction));
  }

  const transactionStatusTotal = customerAnalytics.successfulCount + customerAnalytics.failedCount || 1;
  const successRate = Math.round((customerAnalytics.successfulCount / transactionStatusTotal) * 100);
  const comparisonMax = Math.max(
    customerAnalytics.receivedComparisonTotal,
    customerAnalytics.sentComparisonTotal,
    1,
  );

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Support operations"
        title="Support dashboard"
        description="Compact support view for transactions, customer insights, and audit readiness."
      />

      <div className="support-tab-nav" role="tablist" aria-label="Support sections">
        {SUPPORT_TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            className={`support-tab ${activeTab === tab.key ? 'support-tab--active' : ''}`.trim()}
            aria-selected={activeTab === tab.key}
            onClick={() => setActiveTab(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <ErrorAlert message={error} onDismiss={() => setError('')} />

      {loading ? <LoadingState label="Loading support dashboard..." /> : null}

      {!loading && activeTab === 'transactions' ? (
        <SectionCard title="Transactions" subtitle="Monitor support transactions with customer-level filtering.">
          <div className="filter-row">
            <label className="support-compact-label">
              <span>Customer filter</span>
              <select value={transactionCustomerFilter} onChange={handleTransactionCustomerFilterChange}>
                <option value={ALL_CUSTOMERS}>All Customers</option>
                {customerOptions.map((customer) => (
                  <option key={customer.id} value={String(customer.id)}>
                    {customer.customerName}
                  </option>
                ))}
              </select>
            </label>
          </div>

          {transactionsLoading ? (
            <LoadingState label="Refreshing transactions..." />
          ) : (
            <div className="support-table-compact">
              <DataTable
                rowKey={(transaction) => transaction.transactionId}
                rows={transactions}
                columns={[
                  { key: 'transactionId', label: 'Transaction ID' },
                  { key: 'senderAccountNumber', label: 'Sender Account' },
                  { key: 'receiverAccountNumber', label: 'Receiver Account' },
                  {
                    key: 'amount',
                    label: 'Amount',
                    render: (transaction) => formatCurrency(transaction.amount, getTransactionBaseCurrency(transaction)),
                  },
                  {
                    key: 'currency',
                    label: 'Currency',
                    render: (transaction) => getTransactionBaseCurrency(transaction),
                  },
                  {
                    key: 'paymentStatus',
                    label: 'Status',
                    render: (transaction) => <StatusBadge status={transaction.paymentStatus} />,
                  },
                  {
                    key: 'transactionDate',
                    label: 'Transaction Date',
                    render: (transaction) => formatDateTime(getEffectiveTransactionTime(transaction)),
                  },
                ]}
                empty={<EmptyState title="No transactions found" description="No rows match the selected customer filter." />}
              />
            </div>
          )}
        </SectionCard>
      ) : null}

      {!loading && activeTab === 'customers' ? (
        <>
          <SectionCard title="Customers" subtitle="Select a customer to review profile, accounts, and transaction analytics.">
            <div className="filter-row">
              <label className="support-compact-label">
                <span>Select customer</span>
                <select value={selectedCustomerId} onChange={(event) => setSelectedCustomerId(event.target.value)}>
                  <option value="" disabled>Select a customer</option>
                  {customerOptions.map((customer) => (
                    <option key={customer.id} value={String(customer.id)}>
                      {customer.customerName}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          </SectionCard>

          {customerInsightsLoading ? <LoadingState label="Loading customer insights..." /> : null}

          {!customerInsightsLoading && selectedCustomer ? (
            <>
              <div className="content-grid content-grid--2col">
                <SectionCard title="Customer profile">
                  <div className="detail-grid">
                    <div>
                      <span className="detail-label">Customer Name</span>
                      <strong>{selectedCustomer.customerName}</strong>
                    </div>
                    <div>
                      <span className="detail-label">Customer ID</span>
                      <strong>{selectedCustomer.customerId}</strong>
                    </div>
                    <div>
                      <span className="detail-label">Email</span>
                      <strong>{selectedCustomer.email || '—'}</strong>
                    </div>
                    <div>
                      <span className="detail-label">Phone Number</span>
                      <strong>{selectedCustomer.phoneNumber || '—'}</strong>
                    </div>
                    <div>
                      <span className="detail-label">Number of Accounts</span>
                      <strong>{selectedCustomerAccounts.length}</strong>
                    </div>
                  </div>
                </SectionCard>

                <SectionCard title="Account transaction analytics">
                  <div className="support-summary-grid">
                    <StatCard label="Current Balance" value={formatCurrencyBreakdown(customerAnalytics.totalBalanceByCurrency)} tone="info" />
                    <StatCard label="Total Sent" value={formatCurrencyBreakdown(customerAnalytics.totalSentByCurrency)} tone="warning" />
                    <StatCard label="Total Received" value={formatCurrencyBreakdown(customerAnalytics.totalReceivedByCurrency)} tone="success" />
                    <StatCard label="Transactions" value={customerAnalytics.totalTransactionsCount} />
                  </div>

                  <div className="support-visual-grid">
                    <div className="support-chart-card">
                      <p className="detail-label">Successful vs Failed transactions</p>
                      <div className="support-donut-wrap">
                        <div
                          className="support-donut"
                          style={{
                            background: `conic-gradient(rgba(34,197,94,0.9) ${successRate}%, rgba(248,113,113,0.85) ${successRate}% 100%)`,
                          }}
                          aria-hidden="true"
                        />
                        <div>
                          <strong>{successRate}% success</strong>
                          <p className="muted">
                            {customerAnalytics.successfulCount} successful · {customerAnalytics.failedCount} failed
                          </p>
                        </div>
                      </div>
                    </div>

                    <div className="support-chart-card">
                      <p className="detail-label">Sent vs Received amount</p>
                      <div className="support-bar-stack">
                        <div className="support-bar-row">
                          <span>Received</span>
                          <div className="support-bar-track">
                            <div
                              className="support-bar-fill support-bar-fill--success"
                              style={{ width: `${Math.max((customerAnalytics.receivedComparisonTotal / comparisonMax) * 100, 6)}%` }}
                            />
                          </div>
                        </div>
                        <div className="support-bar-row">
                          <span>Sent</span>
                          <div className="support-bar-track">
                            <div
                              className="support-bar-fill support-bar-fill--warning"
                              style={{ width: `${Math.max((customerAnalytics.sentComparisonTotal / comparisonMax) * 100, 6)}%` }}
                            />
                          </div>
                        </div>
                      </div>
                      <p className="muted">
                        Sent: {formatCurrencyBreakdown(customerAnalytics.totalSentByCurrency)} | Received: {formatCurrencyBreakdown(customerAnalytics.totalReceivedByCurrency)}
                      </p>
                    </div>
                  </div>
                </SectionCard>
              </div>

              <SectionCard title="Account overview">
                {selectedCustomerAccounts.length ? (
                  <div className="support-account-grid">
                    {selectedCustomerAccounts.map((account) => (
                      <article key={account.accountNumber} className="support-account-card">
                        <p className="support-account-title">{account.currency || 'INR'} Account</p>
                        <p className="muted">Account Number: {account.accountNumber}</p>
                        <p className="muted">Bank: {account.bankName}</p>
                        <p className="muted">IFSC: {account.ifscCode}</p>
                        <p className="muted">Currency: {account.currency || 'INR'}</p>
                        <div className="support-account-balance">
                          <span>Balance</span>
                          <strong>{formatCurrency(account.balance, account.currency || 'INR')}</strong>
                        </div>
                      </article>
                    ))}
                  </div>
                ) : (
                  <EmptyState title="No active accounts" description="No active account records were returned for this customer." />
                )}
              </SectionCard>

              <SectionCard title="Customer transactions" subtitle="Real transaction history aggregated from all active customer accounts.">
                <div className="support-table-compact">
                  <DataTable
                    rowKey={(transaction) => transaction.transactionId}
                    rows={selectedCustomerTransactions}
                    columns={[
                      { key: 'transactionId', label: 'Transaction ID' },
                      {
                        key: 'accountNumber',
                        label: 'Account',
                        render: (transaction) => getRelatedAccountNumber(transaction),
                      },
                      {
                        key: 'direction',
                        label: 'Flow',
                        render: (transaction) => renderDirectionBadge(transaction),
                      },
                      { key: 'senderAccountNumber', label: 'Sender' },
                      { key: 'receiverAccountNumber', label: 'Receiver' },
                      {
                        key: 'amount',
                        label: 'Amount',
                        render: (transaction) => getTransactionDisplayAmount(transaction),
                      },
                      {
                        key: 'paymentStatus',
                        label: 'Status',
                        render: (transaction) => <StatusBadge status={transaction.paymentStatus} />,
                      },
                      {
                        key: 'transactionDate',
                        label: 'Transaction Date',
                        render: (transaction) => formatDateTime(getEffectiveTransactionTime(transaction)),
                      },
                    ]}
                    empty={<EmptyState title="No customer transactions" description="This customer has no transaction activity yet." />}
                  />
                </div>
              </SectionCard>
            </>
          ) : null}
        </>
      ) : null}

      {!loading && activeTab === 'audit' ? (
        <SectionCard title="Audit Trail">
          <EmptyState title="Audit trail coming soon" description="This section will host support audit events and timeline views." />
        </SectionCard>
      ) : null}
    </div>
  );
}
