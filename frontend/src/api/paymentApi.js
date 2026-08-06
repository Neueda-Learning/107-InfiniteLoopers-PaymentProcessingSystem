import { apiRequest, DEMO_MODE, isBackendUnavailable } from './http';
import { demoTransactions } from './demoData';

function resolveDemoTransaction(transactionId) {
  const transaction = demoTransactions.find((item) => item.transactionId === transactionId);
  return transaction
    ? Promise.resolve(transaction)
    : Promise.reject(new Error(`Demo transaction not found: ${transactionId}`));
}

export function sendPayment(payload) {
  return apiRequest('/payments/send', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function previewPayment(payload) {
  return apiRequest('/payments/preview', {
    method: 'POST',
    body: JSON.stringify(payload),
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

