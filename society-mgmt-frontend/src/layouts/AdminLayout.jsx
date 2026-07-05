import { useState } from 'react';
import { Link as RouterLink, useLocation } from 'react-router-dom';
import {
  AppBar,
  Avatar,
  Box,
  Button,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Stack,
  Toolbar,
  Typography,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import DashboardIcon from '@mui/icons-material/Dashboard';
import PeopleIcon from '@mui/icons-material/People';
import ApartmentIcon from '@mui/icons-material/Apartment';
import HourglassTopIcon from '@mui/icons-material/HourglassTop';
import PaymentIcon from '@mui/icons-material/Payment';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import TuneIcon from '@mui/icons-material/Tune';
import MoneyOffIcon from '@mui/icons-material/MoneyOff';
import EventIcon from '@mui/icons-material/Event';
import LogoutIcon from '@mui/icons-material/Logout';
import HomeWorkIcon from '@mui/icons-material/HomeWork';

import { useAuth } from '../auth/AuthContext.jsx';

const DRAWER_WIDTH = 240;

const NAV_ITEMS = [
  { label: 'Dashboard', path: '/admin/dashboard', icon: <DashboardIcon fontSize="small" /> },
  { label: 'Members', path: '/admin/members', icon: <PeopleIcon fontSize="small" /> },
  { label: 'Units', path: '/admin/units', icon: <ApartmentIcon fontSize="small" /> },
  { label: 'Payment Queue', path: '/admin/payment-queue', icon: <HourglassTopIcon fontSize="small" /> },
  { label: 'Payments', path: '/admin/payments', icon: <PaymentIcon fontSize="small" /> },
  { label: 'Outstanding', path: '/admin/outstanding', icon: <AccountBalanceWalletIcon fontSize="small" /> },
  { label: 'Generate Charges', path: '/admin/generate-charges', icon: <ReceiptLongIcon fontSize="small" /> },
  { label: 'Charge Config', path: '/admin/charge-config', icon: <TuneIcon fontSize="small" /> },
  { label: 'Expenses', path: '/admin/expenses', icon: <MoneyOffIcon fontSize="small" /> },
  { label: 'Bookings', path: '/admin/bookings', icon: <EventIcon fontSize="small" /> },
];

function DrawerContent({ onClose }) {
  const { user, logout } = useAuth();
  const location = useLocation();

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Branding */}
      <Box sx={{ p: 2.5, pb: 1.5 }}>
        <Stack direction="row" spacing={1.5} alignItems="center">
          <HomeWorkIcon sx={{ color: 'primary.main', fontSize: 28 }} />
          <Box>
            <Typography variant="subtitle1" fontWeight={700} lineHeight={1.2}>
              Society Portal
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Admin Panel
            </Typography>
          </Box>
        </Stack>
      </Box>

      <Divider sx={{ mx: 2, mb: 0.5 }} />

      {/* Navigation */}
      <List sx={{ flex: 1, py: 0.5, overflowY: 'auto' }}>
        {NAV_ITEMS.map((item) => {
          const active =
            location.pathname === item.path ||
            (item.path === '/admin/dashboard' &&
              (location.pathname === '/admin' || location.pathname === '/admin/'));
          return (
            <ListItem key={item.path} disablePadding>
              <ListItemButton
                component={RouterLink}
                to={item.path}
                selected={active}
                onClick={onClose}
                sx={{
                  borderLeft: '3px solid',
                  borderColor: active ? 'primary.main' : 'transparent',
                  pl: active ? '13px' : '16px',
                }}
              >
                <ListItemIcon
                  sx={{ minWidth: 34, color: active ? 'primary.light' : 'text.secondary' }}
                >
                  {item.icon}
                </ListItemIcon>
                <ListItemText
                  primary={item.label}
                  primaryTypographyProps={{
                    fontSize: '0.875rem',
                    fontWeight: active ? 600 : 400,
                    color: active ? 'primary.light' : 'text.primary',
                  }}
                />
              </ListItemButton>
            </ListItem>
          );
        })}
      </List>

      <Divider sx={{ mx: 2 }} />

      {/* User + Logout */}
      <Box sx={{ p: 2 }}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 1.5 }}>
          <Avatar sx={{ width: 34, height: 34, bgcolor: 'primary.dark', fontSize: '0.85rem' }}>
            {user?.fullName?.[0]?.toUpperCase() || 'A'}
          </Avatar>
          <Box sx={{ overflow: 'hidden', flex: 1 }}>
            <Typography variant="body2" fontWeight={600} noWrap>
              {user?.fullName || 'Admin'}
            </Typography>
            <Typography variant="caption" color="text.secondary" noWrap>
              {user?.phone}
            </Typography>
          </Box>
        </Stack>
        <Button
          variant="outlined"
          size="small"
          fullWidth
          startIcon={<LogoutIcon fontSize="small" />}
          onClick={logout}
          sx={{ justifyContent: 'flex-start', minHeight: 36, fontSize: '0.8rem' }}
        >
          Logout
        </Button>
      </Box>
    </Box>
  );
}

export default function AdminLayout({ children }) {
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      {/* Mobile top AppBar (hidden on sm+) */}
      <AppBar
        position="fixed"
        sx={{ display: { sm: 'none' }, zIndex: (t) => t.zIndex.drawer + 1 }}
      >
        <Toolbar>
          <IconButton
            edge="start"
            color="inherit"
            onClick={() => setMobileOpen(true)}
            sx={{ mr: 1 }}
          >
            <MenuIcon />
          </IconButton>
          <HomeWorkIcon sx={{ color: 'primary.main', mr: 1 }} />
          <Typography variant="h6" fontWeight={700}>
            Society Portal
          </Typography>
        </Toolbar>
      </AppBar>

      {/* Mobile Drawer */}
      <Drawer
        variant="temporary"
        open={mobileOpen}
        onClose={() => setMobileOpen(false)}
        ModalProps={{ keepMounted: true }}
        sx={{
          display: { xs: 'block', sm: 'none' },
          '& .MuiDrawer-paper': { width: DRAWER_WIDTH },
        }}
      >
        <DrawerContent onClose={() => setMobileOpen(false)} />
      </Drawer>

      {/* Desktop Permanent Drawer */}
      <Drawer
        variant="permanent"
        sx={{
          display: { xs: 'none', sm: 'block' },
          '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box' },
        }}
        open
      >
        <DrawerContent onClose={() => {}} />
      </Drawer>

      {/* Main content area */}
      <Box
        component="main"
        sx={{
          flex: 1,
          ml: { sm: `${DRAWER_WIDTH}px` },
          mt: { xs: '56px', sm: 0 },
          p: { xs: 2, sm: 3 },
          minHeight: '100vh',
          maxWidth: { sm: `calc(100% - ${DRAWER_WIDTH}px)` },
        }}
        className="page-enter"
      >
        {children}
      </Box>
    </Box>
  );
}
