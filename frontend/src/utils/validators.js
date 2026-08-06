const ACCOUNT_NUMBER_PATTERN = /^\d+$/;
const IFSC_PATTERN = /^[A-Za-z]{4}0[A-Za-z0-9]{6}$/;

export function validatePaymentForm(values, options = {}) {
  const errors = {};
  const knownAccounts = new Set(options.knownAccounts || []);
  const receiverExpectedIfsc = options.receiverExpectedIfsc ? options.receiverExpectedIfsc.toUpperCase() : '';

  const senderAccount = values.senderAccountNumber.trim();
  const receiverAccount = values.receiverAccountNumber.trim();
  const receiverIfsc = values.receiverIfscCode.trim().toUpperCase();

  if (!senderAccount) {
    errors.senderAccountNumber = 'Sender account number is required.';
  } else if (!ACCOUNT_NUMBER_PATTERN.test(senderAccount)) {
    errors.senderAccountNumber = 'Sender account number must contain only digits.';
  } else if (knownAccounts.size > 0 && !knownAccounts.has(senderAccount)) {
    errors.senderAccountNumber = 'Sender account number is not recognized.';
  }

  if (!receiverAccount) {
    errors.receiverAccountNumber = 'Receiver account number is required.';
  } else if (!ACCOUNT_NUMBER_PATTERN.test(receiverAccount)) {
    errors.receiverAccountNumber = 'Receiver account number must contain only digits.';
  } else if (knownAccounts.size > 0 && !knownAccounts.has(receiverAccount)) {
    errors.receiverAccountNumber = 'Receiver account number is not recognized.';
  }

  if (
    senderAccount
    && receiverAccount
    && senderAccount.toLowerCase() === receiverAccount.toLowerCase()
  ) {
    errors.receiverAccountNumber = 'Sender and receiver account numbers cannot be the same.';
  }

  if (!receiverIfsc) {
    errors.receiverIfscCode = 'Receiver IFSC code is required.';
  } else if (!IFSC_PATTERN.test(receiverIfsc)) {
    errors.receiverIfscCode = 'IFSC must be 11 characters (first 4 letters, 5th is 0, last 6 alphanumeric).';
  } else if (receiverExpectedIfsc && receiverIfsc !== receiverExpectedIfsc) {
    errors.receiverIfscCode = 'IFSC does not match the selected receiver account.';
  }

  if (!values.amount) {
    errors.amount = 'Amount is required.';
  } else if (Number(values.amount) <= 0) {
    errors.amount = 'Amount must be greater than zero.';
  }

  if (values.description.length > 255) {
    errors.description = 'Description must not exceed 255 characters.';
  }

  if (!values.upiPin.trim()) {
    errors.upiPin = 'UPI PIN is required.';
  } else if (!/^\d{4}$/.test(values.upiPin.trim())) {
    errors.upiPin = 'UPI PIN must contain exactly 4 numeric digits.';
  }

  return errors;
}

