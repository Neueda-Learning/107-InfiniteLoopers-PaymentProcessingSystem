import { EmptyState, SectionCard } from '../../components/UI';
import { formatCurrency } from '../../utils/formatters';

export function SenderAccountDetails({
  accounts,
  selectedAccountNumber,
  onSelectAccount,
}) {
  const senderAccount = accounts.find((account) => account.accountNumber === selectedAccountNumber) || null;

  return (
    <SectionCard title="Sender details" subtitle="Read-only details for the account to be debited.">
      {!accounts.length ? (
        <EmptyState
          title="No active accounts"
          description="Select a sender customer with active accounts to continue."
        />
      ) : (
        <div className="detail-stack">
          <label>
            <span>Sender account</span>
            <select value={selectedAccountNumber} onChange={(event) => onSelectAccount(event.target.value)}>
              {accounts.map((account) => (
                <option key={account.accountId} value={account.accountNumber}>
                  {account.accountNumber} | {account.bankName}
                </option>
              ))}
            </select>
          </label>

          {senderAccount ? (
            <div className="detail-grid">
              <div>
                <span className="detail-label">Sender Account Number</span>
                <strong>{senderAccount.accountNumber}</strong>
              </div>
              <div>
                <span className="detail-label">Sender IFSC</span>
                <strong>{senderAccount.ifscCode}</strong>
              </div>
              <div>
                <span className="detail-label">Currency</span>
                <strong>{senderAccount.currency}</strong>
              </div>
              <div>
                <span className="detail-label">Available Balance</span>
                <strong>{formatCurrency(senderAccount.balance, senderAccount.currency)}</strong>
              </div>
            </div>
          ) : null}
        </div>
      )}
    </SectionCard>
  );
}

