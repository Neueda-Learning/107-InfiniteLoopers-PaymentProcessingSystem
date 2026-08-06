import { SectionCard, StatusBadge } from '../../components/UI';
import { formatCurrency, formatDateTime } from '../../utils/formatters';

export function PaymentSuccess({ result, onCreateAnother }) {
  if (!result) {
    return null;
  }

  const isFailed = result.paymentStatus === 'FAILED';

  return (
    <SectionCard
      title="Step 5: Transaction result"
      subtitle={isFailed ? 'Your payment could not be processed. See details below.' : 'Transfer confirmation and reference details.'}
    >
      <div className="detail-stack">
        <div className="detail-grid">
          <div>
            <span className="detail-label">Transaction ID</span>
            <strong>{result.transactionId || '—'}</strong>
          </div>
          <div>
            <span className="detail-label">Status</span>
            <StatusBadge status={result.paymentStatus} />
          </div>
          <div>
            <span className="detail-label">Amount</span>
            <strong>{formatCurrency(result.amount, result.senderCurrency)}</strong>
          </div>
          {!isFailed && (
            <div>
              <span className="detail-label">Receiver Amount</span>
              <strong>{formatCurrency(result.convertedAmount, result.receiverCurrency)}</strong>
            </div>
          )}
          <div>
            <span className="detail-label">Transaction Time</span>
            <strong>{formatDateTime(result.transactionTime)}</strong>
          </div>
        </div>

        <p className="muted">{result.message}</p>

        <div>
          <button type="button" className="secondary-button" onClick={onCreateAnother}>
            {isFailed ? 'Try Another Payment' : 'Create Another Payment'}
          </button>
        </div>
      </div>
    </SectionCard>
  );
}
