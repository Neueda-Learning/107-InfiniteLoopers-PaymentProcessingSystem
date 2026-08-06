import { useEffect, useMemo, useState } from 'react';
import {
  getSupportDashboard,
  getSupportTransactions,
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
  buildTransactionTimeline,
  formatCurrency,
  formatDateTime,
  getEffectiveTransactionTime,
} from '../../utils/formatters';
import { getAllCustomers } from '../../api/customerApi';

export function SupportPage() {
  const [dashboard, setDashboard] = useState(null);
  const [customers, setCustomers] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [customerFilter, setCustomerFilter] = useState('ALL');
  const [issueFilter, setIssueFilter] = useState('ALL_ISSUES');
  const [selectedIssueTransaction, setSelectedIssueTransaction] = useState(null);

  async function loadDashboard() {
    const [dashboardData, transactionData, customers] = await Promise.all([
      getSupportDashboard(),
      getSupportTransactions(),
      getAllCustomers(),
    ]);

    setDashboard(dashboardData);
    setTransactions(transactionData);
    setCustomers(customers);
  }

  const customersByAccount = useMemo(
    () =>
      customers.reduce((acc, customer) => {
        if (customer.accountNumber) {
          acc[customer.accountNumber] = customer;
        }

        return acc;
      }, {}),
    [customers],
  );

  function getDirection(transaction, accountNumber) {
    if (transaction.senderAccountNumber === accountNumber) {
      return 'Sent';
    }

    if (transaction.receiverAccountNumber === accountNumber) {
      return 'Received';
    }

    return 'Related';
  }

  function getCounterpartyAccount(transaction, accountNumber) {
    if (transaction.senderAccountNumber === accountNumber) {
      return transaction.receiverAccountNumber;
    }

    return transaction.senderAccountNumber;
  }

  function getCounterpartyLabel(transaction, accountNumber) {
    const counterpartyAccountNumber = getCounterpartyAccount(transaction, accountNumber);
    const counterparty = customersByAccount[counterpartyAccountNumber];

    if (!counterparty) {
      return counterpartyAccountNumber;
    }

    return `${counterparty.customerName} (${counterpartyAccountNumber})`;
  }

  function renderTransferHow(transaction) {
    const primary = transaction.description || 'Account transfer';
    const outcome = transaction.paymentStatus === 'FAILED' ? 'Processing failed' : 'Processed by system';
    return `${primary} · ${outcome}`;
  }

  function renderAmountWithDirection(transaction, accountNumber) {
    const isReceived = transaction.receiverAccountNumber === accountNumber;
    const isSent = transaction.senderAccountNumber === accountNumber;

    if (isReceived) {
      return (
        <span style={{ color: '#22c55e', fontWeight: '600', whiteSpace: 'nowrap' }}>
          +{formatCurrency(transaction.amount)}
        </span>
      );
    }

    if (isSent) {
      return (
        <span style={{ color: '#ef4444', fontWeight: '600', whiteSpace: 'nowrap' }}>
          −{formatCurrency(transaction.amount)}
        </span>
      );
    }

    return formatCurrency(transaction.amount);
  }

  function sortTransactionsByEffectiveTime(transactionA, transactionB) {
    const timeA = new Date(getEffectiveTransactionTime(transactionA)).getTime();
    const timeB = new Date(getEffectiveTransactionTime(transactionB)).getTime();
    return timeB - timeA;
  }

  function getIssueTags(transaction) {
    const tags = [];
    const status = String(transaction.paymentStatus || '').toUpperCase();
    const description = String(transaction.description || '').toLowerCase();

    if (status === 'FAILED') {
      tags.push('FAILED');
    }

    if (status === 'CANCELLED' || status === 'REVERSED') {
      tags.push('CANCELLED');
    }

    if (/invalid|upi pin|ifsc|insufficient|account not found|validation/.test(description)) {
      tags.push('VALIDATION_ISSUE');
    }

    if (/timeout|error|retry|exception|declined|blocked/.test(description)) {
      tags.push('SYSTEM_ISSUE');
    }

    return [...new Set(tags)];
  }

  function getIssueReason(transaction) {
    const tags = getIssueTags(transaction);
    if (tags.includes('FAILED')) {
      return 'Payment failed';
    }
    if (tags.includes('CANCELLED')) {
      return 'Payment cancelled/reversed';
    }
    if (tags.includes('VALIDATION_ISSUE')) {
      return 'Validation issue';
    }
    if (tags.includes('SYSTEM_ISSUE')) {
      return 'System issue';
    }
    return 'General issue';
  }

  function getIssueLabel(tag) {
    switch (tag) {
      case 'FAILED':
        return 'Failed';
      case 'CANCELLED':
        return 'Cancelled';
      case 'VALIDATION_ISSUE':
        return 'Validation';
      case 'SYSTEM_ISSUE':
        return 'System';
      default:
        return tag;
    }
  }

  function getCustomerNameByAccount(accountNumber) {
    return customersByAccount[accountNumber]?.customerName || 'Unknown customer';
  }

  function matchesIssueFilter(transaction) {
    const tags = getIssueTags(transaction);
    if (!tags.length) {
      return false;
    }

    switch (issueFilter) {
      case 'FAILED':
        return tags.includes('FAILED');
      case 'CANCELLED':
        return tags.includes('CANCELLED');
      case 'VALIDATION_ISSUE':
        return tags.includes('VALIDATION_ISSUE');
      case 'SYSTEM_ISSUE':
        return tags.includes('SYSTEM_ISSUE');
      case 'ALL_ISSUES':
      default:
        return true;
    }
  }

  const issueTransactions = useMemo(() => {
    const customerScoped =
      customerFilter === 'ALL'
        ? transactions
        : transactions.filter(
          (item) =>
            item.senderAccountNumber === customerFilter
            || item.receiverAccountNumber === customerFilter,
        );

    return customerScoped
      .filter(matchesIssueFilter)
      .sort(sortTransactionsByEffectiveTime);
  }, [transactions, customerFilter, issueFilter]);

  const customerSections = useMemo(() => {
    const visibleCustomers =
      customerFilter === 'ALL'
        ? customers
        : customers.filter((customer) => customer.accountNumber === customerFilter);

    return visibleCustomers.map((customer) => {
      const relatedTransactions = transactions
        .filter(
          (item) =>
            item.senderAccountNumber === customer.accountNumber
            || item.receiverAccountNumber === customer.accountNumber,
        )
        .sort(sortTransactionsByEffectiveTime);

      const sentTransactions = relatedTransactions.filter(
        (item) => item.senderAccountNumber === customer.accountNumber,
      );
      const receivedTransactions = relatedTransactions.filter(
        (item) => item.receiverAccountNumber === customer.accountNumber,
      );
      const failedTransactions = relatedTransactions.filter((item) => item.paymentStatus === 'FAILED');

      return {
        customer,
        relatedTransactions,
        sentCount: sentTransactions.length,
        receivedCount: receivedTransactions.length,
        failedCount: failedTransactions.length,
        totalSentAmount: sentTransactions.reduce((sum, item) => sum + Number(item.amount || 0), 0),
        totalReceivedAmount: receivedTransactions.reduce((sum, item) => sum + Number(item.amount || 0), 0),
      };
    });
  }, [customers, customerFilter, transactions]);

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

  return (
    <div className="page-stack">
      <PageHeader
        title="Support dashboard"
        description="Track customer-wise transaction activity with full profile and payment details."
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

          <SectionCard title="Customer filter" subtitle="Use the dropdown to view all customer sections or focus on one account.">
            <div className="filter-row">
              <select value={customerFilter} onChange={(event) => setCustomerFilter(event.target.value)}>
                <option value="ALL">All customers</option>
                {customers.map((customer) => (
                  <option key={customer.accountNumber} value={customer.accountNumber}>
                    {customer.customerName} ({customer.accountNumber})
                  </option>
                ))}
              </select>
            </div>
            <p className="muted"></p>
          </SectionCard>

          <SectionCard
            title="Payment issue monitor"
            subtitle="Failed/cancelled/problematic transactions for support triage and developer troubleshooting."
          >
            <div className="filter-row">
              <select value={issueFilter} onChange={(event) => setIssueFilter(event.target.value)}>
                <option value="ALL_ISSUES">All issues</option>
                <option value="FAILED">Failed payments</option>
                <option value="CANCELLED">Cancelled/reversed</option>
                <option value="VALIDATION_ISSUE">Validation issues</option>
                <option value="SYSTEM_ISSUE">System issues</option>
              </select>
            </div>

            <div style={{ marginTop: '16px' }}>
              <DataTable
                rowKey={(transaction) => `issue-${transaction.transactionId}`}
                rows={issueTransactions}
                onRowClick={(transaction) => setSelectedIssueTransaction(transaction)}
                columns={[
                  { key: 'transactionId', label: 'Transaction ID' },
                  {
                    key: 'owner',
                    label: 'Customer',
                    render: (transaction) => `${getCustomerNameByAccount(transaction.senderAccountNumber)} (${transaction.senderAccountNumber})`,
                  },
                  {
                    key: 'counterparty',
                    label: 'Counterparty',
                    render: (transaction) => `${getCustomerNameByAccount(transaction.receiverAccountNumber)} (${transaction.receiverAccountNumber})`,
                  },
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
                    key: 'issueReason',
                    label: 'Issue Type',
                    render: (transaction) => getIssueReason(transaction),
                  },
                  {
                    key: 'when',
                    label: 'When',
                    render: (transaction) => formatDateTime(getEffectiveTransactionTime(transaction)),
                  },
                ]}
                empty={
                  <EmptyState
                    title="No payment issues found"
                    description="No transactions matched the selected issue filter and customer scope."
                  />
                }
              />
            </div>
          </SectionCard>

          {selectedIssueTransaction ? (
            <SectionCard
              title={`Issue details: ${selectedIssueTransaction.transactionId}`}
              subtitle="Detailed failure/cancellation context for support and engineering analysis."
            >
              <div className="detail-grid">
                <div>
                  <span className="detail-label">Status</span>
                  <StatusBadge status={selectedIssueTransaction.paymentStatus} />
                </div>
                <div>
                  <span className="detail-label">Issue reason</span>
                  <strong>{getIssueReason(selectedIssueTransaction)}</strong>
                </div>
                <div>
                  <span className="detail-label">Sender</span>
                  <strong>{selectedIssueTransaction.senderAccountNumber}</strong>
                </div>
                <div>
                  <span className="detail-label">Receiver</span>
                  <strong>{selectedIssueTransaction.receiverAccountNumber}</strong>
                </div>
                <div>
                  <span className="detail-label">Amount</span>
                  <strong>{formatCurrency(selectedIssueTransaction.amount)}</strong>
                </div>
                <div>
                  <span className="detail-label">Latest event time</span>
                  <strong>{formatDateTime(getEffectiveTransactionTime(selectedIssueTransaction))}</strong>
                </div>
                <div className="form-grid__full">
                  <span className="detail-label">Description / Issue note</span>
                  <strong>{selectedIssueTransaction.description || 'No description available'}</strong>
                </div>
                <div className="form-grid__full">
                  <span className="detail-label">Issue tags</span>
                  <div className="button-row">
                    {getIssueTags(selectedIssueTransaction).map((tag) => (
                      <span key={tag} className="badge badge--warning">{getIssueLabel(tag)}</span>
                    ))}
                  </div>
                </div>
              </div>

              <div style={{ marginTop: '16px' }}>
                <h3 className="subheading">Status timeline</h3>
                <div className="timeline">
                  {buildTransactionTimeline(selectedIssueTransaction).map(([label, timestamp]) => (
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
            </SectionCard>
          ) : null}

          {customerSections.length ? (
            customerSections.map((section) => (
              <SectionCard
                key={section.customer.accountNumber}
                title={`${section.customer.customerName} (${section.customer.accountNumber})`}
                subtitle=""
              >
                <div className="detail-grid">
                  <div>
                    <span className="detail-label">Customer</span>
                    <strong>{section.customer.customerName}</strong>
                  </div>
                  <div>
                    <span className="detail-label">Account number</span>
                    <strong>{section.customer.accountNumber}</strong>
                  </div>
                  <div>
                    <span className="detail-label">Bank / IFSC</span>
                    <strong>{section.customer.bankName} ({section.customer.ifscCode})</strong>
                  </div>
                  <div>
                    <span className="detail-label">Current balance</span>
                    <strong>{formatCurrency(section.customer.balance)}</strong>
                  </div>
                  <div>
                    <span className="detail-label">Email</span>
                    <strong>{section.customer.email}</strong>
                  </div>
                  <div>
                    <span className="detail-label">Phone</span>
                    <strong>{section.customer.phoneNumber}</strong>
                  </div>
                </div>

                <div className="stats-grid" style={{ marginTop: '16px' }}>
                  <StatCard label="Total transactions" value={section.relatedTransactions.length} />
                  <StatCard label="Sent" value={section.sentCount} tone="info" />
                  <StatCard label="Received" value={section.receivedCount} tone="success" />
                  <StatCard label="Failed" value={section.failedCount} tone="danger" />
                  <StatCard label="Total sent" value={formatCurrency(section.totalSentAmount)} tone="warning" />
                  <StatCard label="Total received" value={formatCurrency(section.totalReceivedAmount)} tone="success" />
                </div>

                <div style={{ marginTop: '16px' }}>
                  <DataTable
                    rowKey={(transaction) => `${section.customer.accountNumber}-${transaction.transactionId}`}
                    rows={section.relatedTransactions}
                    columns={[
                      { key: 'transactionId', label: 'Transaction ID' },
                      {
                        key: 'direction',
                        label: 'Type',
                        render: (transaction) => getDirection(transaction, section.customer.accountNumber),
                      },
                      {
                        key: 'counterparty',
                        label: 'Whom',
                        render: (transaction) => getCounterpartyLabel(transaction, section.customer.accountNumber),
                      },
                      {
                        key: 'amount',
                        label: 'Amount',
                        render: (transaction) => renderAmountWithDirection(transaction, section.customer.accountNumber),
                      },
                      {
                        key: 'paymentStatus',
                        label: 'Status',
                        render: (transaction) => <StatusBadge status={transaction.paymentStatus} />,
                      },
                      {
                        key: 'when',
                        label: 'When',
                        render: (transaction) => formatDateTime(getEffectiveTransactionTime(transaction)),
                      },
                      {
                        key: 'how',
                        label: 'How',
                        render: renderTransferHow,
                      },
                    ]}
                    empty={
                      <EmptyState
                        title="No transactions for this customer"
                        description="This account has no related transactions in the current dataset."
                      />
                    }
                  />
                </div>
              </SectionCard>
            ))
          ) : (
            <SectionCard title="Customer detail sections" subtitle="No customer records available for the current selection.">
              <EmptyState
                title="No customers found"
                description="Check backend data and then refresh support metrics."
              />
            </SectionCard>
          )}
        </>
      )}
    </div>
  );
}


