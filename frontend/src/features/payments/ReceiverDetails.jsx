import { EmptyState, LoadingState, SectionCard } from '../../components/UI';

export function ReceiverDetails({
  receiverAccountNumber,
  onReceiverAccountNumberChange,
  onLookupReceiver,
  receiver,
  loading,
  disabled,
}) {
  const canLookup = !disabled && receiverAccountNumber.trim().length > 0;

  return (
    <SectionCard title="Step 2: Enter receiver" subtitle="Fetch account details using GET /api/customers/account/{accountNumber}.">
      <div className="detail-stack">
        <label>
          <span>Receiver account number</span>
          <div className="button-row">
            <input
              value={receiverAccountNumber}
              onChange={(event) => onReceiverAccountNumberChange(event.target.value.replace(/\D/g, ''))}
              placeholder="Enter receiver account number"
              inputMode="numeric"
              disabled={disabled}
            />
            <button type="button" className="secondary-button" onClick={onLookupReceiver} disabled={!canLookup || loading}>
              {loading ? 'Fetching...' : 'Fetch Receiver'}
            </button>
          </div>
        </label>

        {loading ? <LoadingState label="Loading receiver details..." /> : null}

        {!loading && receiver ? (
          <div className="detail-grid">
            <div>
              <span className="detail-label">Receiver Account Number</span>
              <strong>{receiver.accountNumber}</strong>
            </div>
            <div>
              <span className="detail-label">Receiver IFSC</span>
              <strong>{receiver.ifscCode}</strong>
            </div>
            <div>
              <span className="detail-label">Receiver Currency</span>
              <strong>{receiver.currency}</strong>
            </div>
          </div>
        ) : null}

        {!loading && !receiver ? (
          <EmptyState
            title="Receiver details pending"
            description="Enter a receiver account number and fetch details to continue."
          />
        ) : null}
      </div>
    </SectionCard>
  );
}

