import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider, CssBaseline, GlobalStyles } from '@mui/material';

import theme from './theme/theme.js';
import { AuthProvider } from './auth/AuthContext.jsx';
import ProtectedRoute from './auth/ProtectedRoute.jsx';
import AdminLayout from './layouts/AdminLayout.jsx';
import MemberLayout from './layouts/MemberLayout.jsx';

import Login from './pages/auth/Login.jsx';
import AdminDashboard from './pages/admin/Dashboard.jsx';
import Members from './pages/admin/Members.jsx';
import Units from './pages/admin/Units.jsx';
import ChargeConfig from './pages/admin/ChargeConfig.jsx';
import GenerateCharges from './pages/admin/GenerateCharges.jsx';
import PaymentQueue from './pages/admin/PaymentQueue.jsx';
import Payments from './pages/admin/Payments.jsx';
import Outstanding from './pages/admin/Outstanding.jsx';
import Expenses from './pages/admin/Expenses.jsx';
import Bookings from './pages/admin/Bookings.jsx';
import MyProfile from './pages/member/MyProfile.jsx';
import MyDues from './pages/member/MyDues.jsx';
import SubmitPayment from './pages/member/SubmitPayment.jsx';
import MyPayments from './pages/member/MyPayments.jsx';
import MyReceipts from './pages/member/MyReceipts.jsx';
import ExpensesView from './pages/member/ExpensesView.jsx';
import MyBookings from './pages/member/MyBookings.jsx';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, refetchOnWindowFocus: false },
  },
});

const globalStyles = {
  '@keyframes gradientShift': {
    '0%, 100%': { backgroundPosition: '0% 50%' },
    '50%': { backgroundPosition: '100% 50%' },
  },
  '@keyframes fadeSlideIn': {
    from: { opacity: 0, transform: 'translateY(10px)' },
    to: { opacity: 1, transform: 'none' },
  },
  body: {
    background: 'linear-gradient(-45deg, #0f0c29, #302b63, #1e3a5f, #0f172a)',
    backgroundSize: '400% 400%',
    animation: 'gradientShift 20s ease infinite',
    backgroundAttachment: 'fixed',
    minHeight: '100vh',
  },
  '.page-enter': {
    animation: 'fadeSlideIn 0.28s ease both',
  },
  '::-webkit-scrollbar': { width: 6, height: 6 },
  '::-webkit-scrollbar-track': { background: 'rgba(255,255,255,0.03)' },
  '::-webkit-scrollbar-thumb': {
    background: 'rgba(255,255,255,0.14)',
    borderRadius: 3,
    '&:hover': { background: 'rgba(255,255,255,0.24)' },
  },
};

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <GlobalStyles styles={globalStyles} />
        <AuthProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/login" element={<Login />} />

              <Route
                path="/admin/*"
                element={
                  <ProtectedRoute role="ADMIN">
                    <AdminLayout>
                      <AdminRoutes />
                    </AdminLayout>
                  </ProtectedRoute>
                }
              />

              <Route
                path="/member/*"
                element={
                  <ProtectedRoute role="MEMBER">
                    <MemberLayout>
                      <MemberRoutes />
                    </MemberLayout>
                  </ProtectedRoute>
                }
              />

              <Route path="/" element={<Navigate to="/login" replace />} />
              <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
}

function AdminRoutes() {
  return (
    <Routes>
      <Route index element={<AdminDashboard />} />
      <Route path="dashboard" element={<AdminDashboard />} />
      <Route path="members" element={<Members />} />
      <Route path="units" element={<Units />} />
      <Route path="charge-config" element={<ChargeConfig />} />
      <Route path="generate-charges" element={<GenerateCharges />} />
      <Route path="payment-queue" element={<PaymentQueue />} />
      <Route path="payments" element={<Payments />} />
      <Route path="outstanding" element={<Outstanding />} />
      <Route path="expenses" element={<Expenses />} />
      <Route path="bookings" element={<Bookings />} />
    </Routes>
  );
}

function MemberRoutes() {
  return (
    <Routes>
      <Route index element={<MyProfile />} />
      <Route path="profile" element={<MyProfile />} />
      <Route path="dues" element={<MyDues />} />
      <Route path="submit-payment" element={<SubmitPayment />} />
      <Route path="payments" element={<MyPayments />} />
      <Route path="receipts" element={<MyReceipts />} />
      <Route path="expenses" element={<ExpensesView />} />
      <Route path="bookings" element={<MyBookings />} />
    </Routes>
  );
}
