import { useState } from 'react';
import { Link as RouterLink, useLocation } from 'react-router-dom';
import {
  AppBar,
  Avatar,
  Box,
  Button,
  Chip,
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
import PersonIcon from '@mui/icons-material/Person';
import ReceiptIcon from '@mui/icons-material/Receipt';
import PaymentIcon from '@mui/icons-material/Payment';
import HistoryIcon from '@mui/icons-material/History';
import ArticleIcon from '@mui/icons-material/Article';
import MoneyOffIcon from '@mui/icons-material/MoneyOff';
import EventAvailableIcon from '@mui/icons-material/EventAvailable';
import LogoutIcon from '@mui/icons-material/Logout';
import HomeWorkIcon from '@mui/icons-material/HomeWork';

import { useAuth } from '../auth/AuthContext.jsx';

const DRAWER_WIDTH = 240;

const NAV_ITEMS = [
  { label: 'Profile', path: '/member/profile', icon: <PersonIcon fontSize="small" /> },
  { label: 'My Dues', path: '/member/dues', icon: <ReceiptIcon fontSize="small" /> },
  { label: 'Submit Payment', path: '/member/submit-payment', icon: <PaymentIcon fontSize="small" /> },
  { label: 'My Payments', path: '/member/payments', icon: <HistoryIcon fontSize="small" /> },
  { label: 'Receipts', path: '/member/receipts', icon: <ArticleIcon fontSize="small" /> },
  { label: 'Society Expenses', path: '/member/expenses', icon: <MoneyOffIcon fontSize="small" /> },
  { label: 'Terrace Booking', path: '/member/bookings', icon: <EventAvailableIcon fontSize="small" /> },
];

function isActive(path, location) {
  return (
    location.pathname === path ||
    (path === '/member/profile' &&
      (location.pathname === '/member' || location.pathname === '/member/'))
  );
}

function MobileDrawer({ open, onClose }) {
  const { user, logout } = useAuth();
  const location = useLocation();

  return (
    <Drawer
      variant="temporary"
      open={open}
      onClose={onClose}
      ModalProps={{ keepMounted: true }}
      sx={{
        display: { xs: 'block', sm: 'none' },
        '& .MuiDrawer-paper': { width: DRAWER_WIDTH },
      }}
    >
      <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
        <Box sx={{ p: 2.5, pb: 1.5 }}>
          <Stack direction="row" spacing={1.5} alignItems="center">
            <HomeWorkIcon sx={{ color: 'primary.main', fontSize: 28 }} />
            <Box>
              <Typography variant="subtitle1" fontWeight={700} lineHeight={1.2}>
                Society Portal
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Member Area
              </Typography>
            </Box>
          </Stack>
        </Box>

        <Divider sx={{ mx: 2, mb: 0.5 }} />

        <List sx={{ flex: 1, py: 0.5 }}>
          {NAV_ITEMS.map((item) => {
            const active = isActive(item.path, location);
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

        <Box sx={{ p: 2 }}>
          <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 1.5 }}>
            <Avatar sx={{ width: 34, height: 34, bgcolor: 'secondary.dark', fontSize: '0.85rem' }}>
              {user?.fullName?.[0]?.toUpperCase() || 'M'}
            </Avatar>
            <Box sx={{ overflow: 'hidden', flex: 1 }}>
              <Typography variant="body2" fontWeight={600} noWrap>
                {user?.fullName || 'Member'}
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
    </Drawer>
  );
}

export default function MemberLayout({ children }) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const { user, logout } = useAuth();
  const location = useLocation();

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      {/* Sticky top AppBar */}
      <AppBar position="fixed" sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}>
        <Toolbar sx={{ gap: 1 }}>
          {/* Mobile hamburger */}
          <IconButton
            edge="start"
            color="inherit"
            onClick={() => setMobileOpen(true)}
            sx={{ display: { sm: 'none' } }}
          >
            <MenuIcon />
          </IconButton>

          <HomeWorkIcon sx={{ color: 'primary.main' }} />
          <Typography variant="subtitle1" fontWeight={700} sx={{ mr: 1 }}>
            Society Portal
          </Typography>

          {/* Desktop nav items */}
          <Stack
            direction="row"
            spacing={0.5}
            sx={{ display: { xs: 'none', sm: 'flex' }, flex: 1, overflowX: 'auto' }}
          >
            {NAV_ITEMS.map((item) => {
              const active = isActive(item.path, location);
              return (
                <Button
                  key={item.path}
                  component={RouterLink}
                  to={item.path}
                  size="small"
                  startIcon={item.icon}
                  sx={{
                    color: active ? 'primary.light' : 'text.secondary',
                    background: active ? 'rgba(129,140,248,0.15)' : 'transparent',
                    border: '1px solid',
                    borderColor: active ? 'rgba(129,140,248,0.35)' : 'transparent',
                    minHeight: 34,
                    px: 1.5,
                    fontSize: '0.78rem',
                    fontWeight: active ? 600 : 400,
                    whiteSpace: 'nowrap',
                    flexShrink: 0,
                    '&:hover': {
                      background: 'rgba(255,255,255,0.08)',
                      color: 'text.primary',
                      borderColor: 'rgba(255,255,255,0.2)',
                    },
                  }}
                >
                  {item.label}
                </Button>
              );
            })}
          </Stack>

          {/* User chip + logout */}
          <Stack direction="row" spacing={0.5} alignItems="center" sx={{ ml: 'auto' }}>
            <Chip
              avatar={
                <Avatar sx={{ bgcolor: 'secondary.dark', fontSize: '0.7rem !important' }}>
                  {user?.fullName?.[0]?.toUpperCase() || 'M'}
                </Avatar>
              }
              label={user?.fullName || 'Member'}
              size="small"
              sx={{ display: { xs: 'none', md: 'flex' }, fontSize: '0.78rem' }}
            />
            <IconButton
              color="inherit"
              onClick={logout}
              size="small"
              title="Logout"
              sx={{ ml: 0.5 }}
            >
              <LogoutIcon fontSize="small" />
            </IconButton>
          </Stack>
        </Toolbar>
      </AppBar>

      {/* Mobile Drawer */}
      <MobileDrawer open={mobileOpen} onClose={() => setMobileOpen(false)} />

      {/* Page content below AppBar */}
      <Box
        component="main"
        sx={{ flex: 1, mt: '64px', p: { xs: 2, sm: 3 } }}
        className="page-enter"
      >
        {children}
      </Box>
    </Box>
  );
}
