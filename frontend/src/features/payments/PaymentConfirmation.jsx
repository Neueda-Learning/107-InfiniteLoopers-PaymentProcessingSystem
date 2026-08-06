import { SectionCard } from '../../components/UI';

export function PaymentConfirmation({
  upiPin,
  onUpiPinChange,
  description,
  onDescriptionChange,
  onSend,
  sending,
  disabled,
  canSubmit,
}) {
  return (
    <SectionCard title="Step 4: Confirm and send" subtitle="Enter UPI PIN and submit using POST /api/payments/send.">
      <form
        className="form-grid"
        onSubmit={(event) => {
          event.preventDefault();
          onSend();
        }}
      >
        <label>
          <span>UPI PIN</span>
          <input
            value={upiPin}
            onChange={(event) => onUpiPinChange(event.target.value.replace(/\D/g, '').slice(0, 4))}
            type="password"
            inputMode="numeric"
            maxLength="4"
            placeholder="Enter 4-digit UPI PIN"
            disabled={disabled || sending}
          />
        </label>

        <label>
          <span>Description (optional)</span>
          <input
            value={description}
            onChange={(event) => onDescriptionChange(event.target.value.slice(0, 255))}
            placeholder="Payment note"
            disabled={disabled || sending}
          />
        </label>

        <div className="form-grid__full button-row">
          <button type="submit" className="primary-button" disabled={!canSubmit || sending}>
            {sending ? 'Sending Payment...' : 'Send Payment'}
          </button>
        </div>
      </form>
    </SectionCard>
  );
}


