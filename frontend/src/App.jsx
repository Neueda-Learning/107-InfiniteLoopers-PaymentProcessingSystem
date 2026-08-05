import { Navigate, Route, Routes } from 'react-router-dom';
import { AppShell } from './components/AppShell';
import { OverviewPage } from './features/overview/OverviewPage';
import { PaymentPage } from './features/payments/PaymentPage';
import { TransactionsPage } from './features/transactions/TransactionsPage';
import { CustomersPage } from './features/customers/CustomersPage';
import { SupportPage } from './features/support/SupportPage';

export default function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<Navigate to="/overview" replace />} />
        <Route path="/overview" element={<OverviewPage />} />
        <Route path="/payments" element={<PaymentPage />} />
        <Route path="/transactions" element={<TransactionsPage />} />
        <Route path="/customers" element={<CustomersPage />} />
        <Route path="/support" element={<SupportPage />} />
      </Route>
    </Routes>
  );
}

