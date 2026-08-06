export const PAYMENT_STATUSES = ['CREATED', 'VALIDATED', 'SENT', 'COMPLETED', 'FAILED'];

export function formatCurrency(value, currency = 'INR') {
  const amount = Number(value ?? 0);
  const safeCurrency = typeof currency === 'string' && currency.trim() ? currency : 'INR';

  const locale = safeCurrency === 'INR' ? 'en-IN' : 'en-US';

  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: safeCurrency,
    maximumFractionDigits: 2,
  }).format(Number.isFinite(amount) ? amount : 0);
}

export function formatDateTime(value) {
  if (!value) {
    return '—';
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat('en-IN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date);
}

export function prettifyStatus(status) {
  if (!status) {
    return 'Unknown';
  }

  return status
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

export function getStatusTone(status) {
  switch (status) {
    case 'COMPLETED':
      return 'success';
    case 'FAILED':
      return 'danger';
    case 'SENT':
      return 'info';
    case 'VALIDATED':
      return 'warning';
    case 'CREATED':
    default:
      return 'neutral';
  }
}

export function getEffectiveTransactionTime(transaction) {
  return (
    transaction.completedTime ||
    transaction.failedTime ||
    transaction.sentTime ||
    transaction.validatedTime ||
    transaction.createdTime ||
    transaction.transactionTime ||
    null
  );
}

export function buildTransactionTimeline(transaction) {
  return [
    ['Created', transaction.createdTime],
    ['Validated', transaction.validatedTime],
    ['Sent', transaction.sentTime],
    ['Completed', transaction.completedTime],
    ['Failed', transaction.failedTime],
  ].filter(([, timestamp]) => Boolean(timestamp));
}

