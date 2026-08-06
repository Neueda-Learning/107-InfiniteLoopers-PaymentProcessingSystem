import { useEffect, useState } from 'react';
import {
  getAllCustomers,
  getCustomerByAccount,
  getCustomerById,
  getCustomerTransactions,
  getTransactionForCustomer,
} from '../../api/customerApi';
import {
  DataTable,
  EmptyState,
  ErrorAlert,
  LoadingState,
  PageHeader,
  SectionCard,
  StatusBadge,
} from '../../components/UI';
import { formatCurrency, formatDateTime, getEffectiveTransactionTime } from '../../utils/formatters';

export function CustomersPage() {
  const [customers, setCustomers] = useState([]);
  const [selectedCustomer, setSelectedCustomer] = useState(null);
  const [history, setHistory] = useState([]);
  const [selectedTransaction, setSelectedTransaction] = useState(null);
  const [loading, setLoading] = useState(true);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [transactionLoading, setTransactionLoading] = useState(false);
  const [error, setError] = useState('');
  const [accountLookup, setAccountLookup] = useState('');
  const [customerIdLookup, setCustomerIdLookup] = useState('');
  const [lookupType, setLookupType] = useState('account');
  const [lookupValue, setLookupValue] = useState('');

  useEffect(() => {
    let active = true;

    async function loadCustomers() {
      setLoading(true);
      setError('');

      try {
        const data = await getAllCustomers();
        if (!active) {
          return;
        }

        setCustomers(data);
        if (data.length) {
          handleCustomerSelection(data[0]);
        }
      } catch (err) {
        if (active) {
          setError(err.message || 'Unable to load customers.');
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadCustomers();
    return () => {
      active = false;
    };
  }, []);

  async function handleCustomerSelection(customer) {
    setSelectedCustomer(customer);
    setHistory([]);
    setSelectedTransaction(null);
    setHistoryLoading(true);
    setError('');

    try {
      const transactionHistory = await getCustomerTransactions(customer.accountNumber);
      setHistory(transactionHistory);
    } catch (err) {
      setError(err.message || 'Unable to load customer transaction history.');
    } finally {
      setHistoryLoading(false);
    }
  }

  async function handleCustomerLookup(event) {
    event.preventDefault();
    if (!lookupValue.trim()) {
      return;
    }

    setError('');

    try {
      const customer =
        lookupType === 'account'
          ? await getCustomerByAccount(lookupValue.trim())
          : await getCustomerById(lookupValue.trim());
      setSelectedCustomer(customer);
      await handleCustomerSelection(customer);
    } catch (err) {
      setError(
        err.message || `Customer lookup by ${lookupType === 'account' ? 'account number' : 'ID'} failed.`,
      );
    }
  }

  async function handleTransactionSelection(transactionId) {
    setTransactionLoading(true);
    setError('');

    try {
      const transaction = await getTransactionForCustomer(transactionId);
      setSelectedTransaction(transaction);
    } catch (err) {
      setError(err.message || 'Unable to load transaction detail.');
    } finally {
      setTransactionLoading(false);
    }
  }

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Customer data"
        title="Customers"
        description="Browse customer records, look up accounts, and inspect transaction history from the customer-facing endpoints."
      />

      <ErrorAlert message={error} onDismiss={() => setError('')} />

      <div className="content-grid content-grid--3col">
        <SectionCard title="Find Customer" subtitle="Choose a lookup method and enter the value.">
          <form className="stack-form" onSubmit={handleCustomerLookup}>
            <label>
              <span>Lookup type</span>
              <select value={lookupType} onChange={(event) => setLookupType(event.target.value)}>
                <option value="account">Account number</option>
                <option value="id">Customer ID</option>
              </select>
            </label>

            <label>
              <span>
                {lookupType === 'account' ? 'Account number' : 'Customer ID'}
              </span>
              <input
                value={lookupValue}
                onChange={(event) => setLookupValue(event.target.value)}
                placeholder={lookupType === 'account' ? '100000000001' : '1'}
              />
            </label>

            <button type="submit" className="secondary-button">
              Search
            </button>
          </form>
        </SectionCard>

        <SectionCard title="Customer directory" className="span-2">
          {loading ? (
            <LoadingState label="Loading customers..." />
          ) : (
            <DataTable
              rowKey={(customer) => customer.customerId}
              rows={customers}
              onRowClick={handleCustomerSelection}
              columns={[
                { key: 'customerId', label: 'ID' },
                { key: 'customerName', label: 'Name' },
                { key: 'email', label: 'Email' },
                { key: 'accountNumber', label: 'Account' },
                { key: 'bankName', label: 'Bank' },
                {
                  key: 'balance',
                  label: 'Balance',
                  render: (customer) => formatCurrency(customer.balance),
                },
              ]}
              empty={<EmptyState title="No customers found" description="Check your backend database seed data." />}
            />
          )}
        </SectionCard>
      </div>

      <div className="content-grid content-grid--2col">
        <SectionCard title="Selected customer">
          {selectedCustomer ? (
            <div className="detail-grid">
              <div>
                <span className="detail-label">Customer ID</span>
                <strong>{selectedCustomer.customerId}</strong>
              </div>
              <div>
                <span className="detail-label">Customer name</span>
                <strong>{selectedCustomer.customerName}</strong>
              </div>
              <div>
                <span className="detail-label">Email</span>
                <strong>{selectedCustomer.email}</strong>
              </div>
              <div>
                <span className="detail-label">Phone</span>
                <strong>{selectedCustomer.phoneNumber}</strong>
              </div>
              <div>
                <span className="detail-label">Account number</span>
                <strong>{selectedCustomer.accountNumber}</strong>
              </div>
              <div>
                <span className="detail-label">IFSC code</span>
                <strong>{selectedCustomer.ifscCode}</strong>
              </div>
              <div>
                <span className="detail-label">Bank name</span>
                <strong>{selectedCustomer.bankName}</strong>
              </div>
              <div>
                <span className="detail-label">Balance</span>
                <strong>{formatCurrency(selectedCustomer.balance)}</strong>
              </div>
            </div>
          ) : (
            <EmptyState title="No customer selected" description="Choose a customer from the directory or lookup form." />
          )}
        </SectionCard>

        <SectionCard title="Transaction preview">
          {transactionLoading ? (
            <LoadingState label="Loading transaction detail..." />
          ) : selectedTransaction ? (
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
                <span className="detail-label">Latest time</span>
                <strong>{formatDateTime(getEffectiveTransactionTime(selectedTransaction))}</strong>
              </div>
            </div>
          ) : (
            <EmptyState title="No transaction selected" description="Click a row in the transaction history to inspect it here." />
          )}
        </SectionCard>
      </div>

      <SectionCard title="Transaction history">
        {historyLoading ? (
          <LoadingState label="Loading transaction history..." />
        ) : (
          <DataTable
            rowKey={(transaction) => transaction.transactionId}
            rows={history}
            onRowClick={(transaction) => handleTransactionSelection(transaction.transactionId)}
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
            empty={<EmptyState title="No history found" description="This customer has no transactions yet." />}
          />
        )}
      </SectionCard>
    </div>
  );
}

