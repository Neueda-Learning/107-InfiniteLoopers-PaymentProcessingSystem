import { EmptyState, SectionCard } from '../../components/UI';
import { formatCurrency } from '../../utils/formatters';

function formatRate(value) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }

  const numericRate = Number(value);
  return Number.isFinite(numericRate) ? numericRate.toLocaleString('en-US') : String(value);
}

export function PaymentPreview({
  amount,
  onAmountChange,
  onPreview,
  preview,
  isPreviewing,
  senderCurrency,
  receiverCurrency,
  disabled,
}) {
  const canPreview = !disabled && Number(amount) > 0;

  return (
    <SectionCard title="Step 3: Preview conversion and charges" subtitle="Preview uses POST /api/payments/preview before confirmation.">
      <div className="detail-stack">
        <label>
          <span>Amount{senderCurrency ? ` (${senderCurrency})` : ''}</span>
          <div className="button-row">
            <input
              value={amount}
              onChange={(event) => onAmountChange(event.target.value)}
              type="number"
              min="0"
              step="0.01"
              placeholder="Enter transfer amount"
              disabled={disabled}
            />
            <button type="button" className="secondary-button" onClick={onPreview} disabled={!canPreview || isPreviewing}>
              {isPreviewing ? 'Previewing...' : 'Preview Payment'}
            </button>
          </div>
        </label>

        {preview ? (
          <div className="detail-stack preview-panel">
            <h3 className="subheading">Payment summary</h3>
            <div className="detail-grid">
              <div>
                <span className="detail-label">Sender Currency</span>
                <strong>{preview.senderCurrency}</strong>
              </div>
              <div>
                <span className="detail-label">Receiver Currency</span>
                <strong>{preview.receiverCurrency}</strong>
              </div>
              {preview.conversionRequired ? (
                <>
                  <div>
                    <span className="detail-label">Exchange Rate</span>
                    <strong>{formatRate(preview.exchangeRate)}</strong>
                  </div>
                  <div>
                    <span className="detail-label">Receiver Gets</span>
                    <strong>{formatCurrency(preview.convertedAmount, preview.receiverCurrency)}</strong>
                  </div>
                </>
              ) : (
                <>
                  <div>
                    <span className="detail-label">Currency</span>
                    <strong>{senderCurrency} to {receiverCurrency}</strong>
                  </div>
                  <div>
                    <span className="detail-label">Conversion</span>
                    <strong>Not Required</strong>
                  </div>
                </>
              )}
              <div>
                <span className="detail-label">Sending Amount</span>
                <strong>{formatCurrency(preview.originalAmount, preview.senderCurrency)}</strong>
              </div>
              <div>
                <span className="detail-label">Transfer Charge</span>
                <strong>{formatCurrency(preview.transferCharge, preview.senderCurrency)}</strong>
              </div>
              <div>
                <span className="detail-label">Total Deducted</span>
                <strong>{formatCurrency(preview.totalDeducted, preview.senderCurrency)}</strong>
              </div>
            </div>
          </div>
        ) : (
          <EmptyState
            title="Payment preview pending"
            description="Enter amount and generate a preview to continue to confirmation."
          />
        )}
      </div>
    </SectionCard>
  );
}

