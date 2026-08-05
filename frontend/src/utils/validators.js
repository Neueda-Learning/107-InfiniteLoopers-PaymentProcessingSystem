export function validatePaymentForm(values) {
  const errors = {};

  if (!values.senderAccountNumber.trim()) {
    errors.senderAccountNumber = 'Sender account number is required.';
  }

  if (!values.receiverAccountNumber.trim()) {
    errors.receiverAccountNumber = 'Receiver account number is required.';
  }

  if (
    values.senderAccountNumber.trim() &&
    values.receiverAccountNumber.trim() &&
    values.senderAccountNumber.trim().toLowerCase() === values.receiverAccountNumber.trim().toLowerCase()
  ) {
    errors.receiverAccountNumber = 'Sender and receiver account numbers cannot be the same.';
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

