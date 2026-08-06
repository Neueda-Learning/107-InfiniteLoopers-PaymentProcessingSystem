import { apiRequest, DEMO_MODE, isBackendUnavailable } from './http';
import { demoCustomers, demoTransactions } from './demoData';

function findDemoCustomerById(customerId) {
  const customer = demoCustomers.find((item) => String(item.customerId) === String(customerId));
  return customer
    ? Promise.resolve(customer)
    : Promise.reject(new Error(`Demo customer not found: ${customerId}`));
}

function findDemoCustomerByAccount(accountNumber) {
  const customer = demoCustomers.find((item) => item.accountNumber === accountNumber);
  return customer
    ? Promise.resolve(customer)
    : Promise.reject(new Error(`Demo account not found: ${accountNumber}`));
}

function findDemoTransactions(accountNumber) {
  return Promise.resolve(
    demoTransactions.filter(
      (item) => item.senderAccountNumber === accountNumber || item.receiverAccountNumber === accountNumber,
    ),
  );
}

function findDemoTransaction(transactionId) {
  const transaction = demoTransactions.find((item) => item.transactionId === transactionId);
  return transaction
    ? Promise.resolve(transaction)
    : Promise.reject(new Error(`Demo transaction not found: ${transactionId}`));
}

export function getAllCustomers() {
  if (DEMO_MODE) {
    return Promise.resolve(demoCustomers);
  }

  return apiRequest('/customers').catch((error) => {
    if (isBackendUnavailable(error)) {
      return demoCustomers;
    }

    throw error;
  });
}

// Payment-flow specific helpers mapped to backend DTOs.
export async function getCustomers() {
  const response = await apiRequest('/customers');

  return (response || []).map((customer) => ({
    id: customer.id,
    customerName: customer.customerName,
  }));
}

export async function getCustomerAccounts(customerId) {
  const response = await apiRequest(`/customers/${customerId}/accounts`);

  return (response || []).map((account) => ({
    accountId: account.accountId,
    accountNumber: account.accountNumber,
    bankName: account.bankName,
    ifscCode: account.ifscCode,
    balance: account.balance,
    currency: account.currency,
    isActive: Boolean(account.isActive),
  }));
}

export function getCustomerById(customerId) {
  if (DEMO_MODE) {
    return findDemoCustomerById(customerId);
  }

  return apiRequest(`/customers/${customerId}`).catch((error) => {
    if (isBackendUnavailable(error)) {
      return findDemoCustomerById(customerId);
    }

    throw error;
  });
}

export function getCustomerByAccount(accountNumber) {
  if (DEMO_MODE) {
    return findDemoCustomerByAccount(accountNumber);
  }

  return apiRequest(`/customers/account/${accountNumber}`).catch((error) => {
    if (isBackendUnavailable(error)) {
      return findDemoCustomerByAccount(accountNumber);
    }

    throw error;
  });
}

export async function getAccountByNumber(accountNumber) {
  const response = await apiRequest(`/customers/account/${accountNumber}`);

  return {
    customerId: response?.customerId,
    customerName: response?.customerName,
    accountNumber: response?.accountNumber,
    ifscCode: response?.ifscCode,
    bankName: response?.bankName,
    balance: response?.balance,
    currency: response?.currency,
  };
}

export function getCustomerTransactions(accountNumber) {
  if (DEMO_MODE) {
    return findDemoTransactions(accountNumber);
  }

  return apiRequest(`/customers/${accountNumber}/transactions`).catch((error) => {
    if (isBackendUnavailable(error)) {
      return findDemoTransactions(accountNumber);
    }

    throw error;
  });
}

export function getTransactionForCustomer(transactionId) {
  if (DEMO_MODE) {
    return findDemoTransaction(transactionId);
  }

  return apiRequest(`/customers/transaction/${transactionId}`).catch((error) => {
    if (isBackendUnavailable(error)) {
      return findDemoTransaction(transactionId);
    }

    throw error;
  });
}

