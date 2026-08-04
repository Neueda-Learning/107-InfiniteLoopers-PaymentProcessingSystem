export const demoCustomers = [
  {
    customerId: 1,
    customerName: 'Alice Johnson',
    email: 'alice.johnson@example.com',
    phoneNumber: '9876543210',
    accountNumber: '100000000001',
    ifscCode: 'SBIN0001234',
    bankName: 'State Bank of India',
    balance: 50000,
  },
  {
    customerId: 2,
    customerName: 'Bob Smith',
    email: 'bob.smith@example.com',
    phoneNumber: '9876501234',
    accountNumber: '100000000002',
    ifscCode: 'HDFC0005678',
    bankName: 'HDFC Bank',
    balance: 30000,
  },
];

export const demoTransactions = [
  {
    transactionId: 'TXN-00000000000000000000000000000001',
    senderAccountNumber: '100000000001',
    receiverAccountNumber: '100000000002',
    amount: 1500,
    description: 'Dummy successful transfer',
    paymentStatus: 'COMPLETED',
    createdTime: '2026-08-03T10:00:00',
    validatedTime: '2026-08-03T10:00:05',
    sentTime: '2026-08-03T10:00:07',
    completedTime: '2026-08-03T10:00:10',
    failedTime: null,
  },
  {
    transactionId: 'TXN-00000000000000000000000000000002',
    senderAccountNumber: '100000000002',
    receiverAccountNumber: '100000000001',
    amount: 700,
    description: 'Dummy failed transfer',
    paymentStatus: 'FAILED',
    createdTime: '2026-08-03T11:00:00',
    validatedTime: '2026-08-03T11:00:03',
    sentTime: null,
    completedTime: null,
    failedTime: '2026-08-03T11:00:08',
  },
];

export const demoDashboard = {
  totalCustomers: 2,
  totalTransactions: 2,
  successfulTransactions: 1,
  failedTransactions: 1,
  totalCreditAmount: 1500,
  totalDebitAmount: 1500,
};

