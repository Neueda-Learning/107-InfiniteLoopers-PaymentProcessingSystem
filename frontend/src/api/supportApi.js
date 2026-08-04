import { apiRequest, DEMO_MODE, isBackendUnavailable } from './http';
import { demoDashboard, demoTransactions } from './demoData';

function findDemoTransactionsByCustomer(accountNumber) {
  return Promise.resolve(
    demoTransactions.filter(
      (item) => item.senderAccountNumber === accountNumber || item.receiverAccountNumber === accountNumber,
    ),
  );
}

function findDemoTransactionsByStatus(status) {
  return Promise.resolve(demoTransactions.filter((item) => item.paymentStatus === status));
}

export function getSupportDashboard() {
  if (DEMO_MODE) {
    return Promise.resolve(demoDashboard);
  }

  return apiRequest('/support/dashboard').catch((error) => {
    if (isBackendUnavailable(error)) {
      return demoDashboard;
    }

    throw error;
  });
}

export function getSupportTransactions() {
  if (DEMO_MODE) {
    return Promise.resolve(demoTransactions);
  }

  return apiRequest('/support/transactions').catch((error) => {
    if (isBackendUnavailable(error)) {
      return demoTransactions;
    }

    throw error;
  });
}

export function getSupportTransactionsByCustomer(accountNumber) {
  if (DEMO_MODE) {
    return findDemoTransactionsByCustomer(accountNumber);
  }

  return apiRequest(`/support/customer/${accountNumber}`).catch((error) => {
    if (isBackendUnavailable(error)) {
      return findDemoTransactionsByCustomer(accountNumber);
    }

    throw error;
  });
}

export function getSupportTransactionsByStatus(status) {
  if (DEMO_MODE) {
    return findDemoTransactionsByStatus(status);
  }

  return apiRequest(`/support/status/${status}`).catch((error) => {
    if (isBackendUnavailable(error)) {
      return findDemoTransactionsByStatus(status);
    }

    throw error;
  });
}

