import { apiRequest, DEMO_MODE, isBackendUnavailable } from './http';
import { demoTransactions } from './demoData';

function resolveDemoTransaction(transactionId) {
  const transaction = demoTransactions.find((item) => item.transactionId === transactionId);
  return transaction
    ? Promise.resolve(transaction)
    : Promise.reject(new Error(`Demo transaction not found: ${transactionId}`));
}

export function sendPayment(payload) {
  if (DEMO_MODE) {
    return Promise.resolve({
      transactionId: `TXN-DEMO-${Date.now()}`,
      paymentStatus: 'COMPLETED',
      message: 'Demo payment completed locally. Start the backend to persist real transactions.',
      amount: Number(payload.amount),
      senderAccountNumber: payload.senderAccountNumber,
      receiverAccountNumber: payload.receiverAccountNumber,
      transactionTime: new Date().toISOString(),
    });
  }

  return apiRequest('/payments/send', {
    method: 'POST',
    body: JSON.stringify(payload),
  }).catch((error) => {
    if (isBackendUnavailable(error)) {
      return {
        transactionId: `TXN-DEMO-${Date.now()}`,
        paymentStatus: 'COMPLETED',
        message: 'Backend is offline, so this demo payment was completed locally in the frontend.',
        amount: Number(payload.amount),
        senderAccountNumber: payload.senderAccountNumber,
        receiverAccountNumber: payload.receiverAccountNumber,
        transactionTime: new Date().toISOString(),
      };
    }

    throw error;
  });
}

export function retryPayment(transactionId) {
  if (DEMO_MODE) {
    return Promise.resolve({
      transactionId: `TXN-DEMO-RETRY-${Date.now()}`,
      paymentStatus: 'COMPLETED',
      message: `Demo retry completed for ${transactionId}. Start the backend for real retry behavior.`,
      amount: 700,
      senderAccountNumber: '100000000002',
      receiverAccountNumber: '100000000001',
      transactionTime: new Date().toISOString(),
    });
  }

  return apiRequest(`/payments/retry/${transactionId}`, {
    method: 'POST',
  }).catch((error) => {
    if (isBackendUnavailable(error)) {
      return {
        transactionId: `TXN-DEMO-RETRY-${Date.now()}`,
        paymentStatus: 'COMPLETED',
        message: `Backend is offline, so this demo retry was completed locally for ${transactionId}.`,
        amount: 700,
        senderAccountNumber: '100000000002',
        receiverAccountNumber: '100000000001',
        transactionTime: new Date().toISOString(),
      };
    }

    throw error;
  });
}

export function getAllTransactions() {
  if (DEMO_MODE) {
    return Promise.resolve(demoTransactions);
  }

  return apiRequest('/payments').catch((error) => {
    if (isBackendUnavailable(error)) {
      return demoTransactions;
    }

    throw error;
  });
}

export function getTransactionById(transactionId) {
  if (DEMO_MODE) {
    return resolveDemoTransaction(transactionId);
  }

  return apiRequest(`/payments/${transactionId}`).catch((error) => {
    if (isBackendUnavailable(error)) {
      return resolveDemoTransaction(transactionId);
    }

    throw error;
  });
}

