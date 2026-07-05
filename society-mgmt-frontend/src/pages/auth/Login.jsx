import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Container,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import HomeWorkIcon from '@mui/icons-material/HomeWork';

import { useAuth } from '../../auth/AuthContext.jsx';
import { loginApi } from '../../api/auth.js';

export default function Login() {
  const navigate = useNavigate();
  const auth = useAuth();

  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const data = await loginApi({ phone, password });
      auth.login(data);
      const destination =
        data?.member?.role === 'ADMIN' ? '/admin/dashboard' : '/member/profile';
      navigate(destination, { replace: true });
    } catch (err) {
      setError(err?.response?.data?.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}
      className="page-enter"
    >
      <Container maxWidth="xs">
        {/* Society branding above the card */}
        <Stack alignItems="center" spacing={1} sx={{ mb: 3 }}>
          <Box
            sx={{
              width: 64,
              height: 64,
              borderRadius: '50%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              background: 'rgba(129, 140, 248, 0.2)',
              border: '1px solid rgba(129, 140, 248, 0.35)',
              boxShadow: '0 0 32px rgba(129, 140, 248, 0.25)',
            }}
          >
            <HomeWorkIcon sx={{ fontSize: 34, color: 'primary.light' }} />
          </Box>
          <Typography
            variant="h5"
            fontWeight={700}
            sx={{
              background: 'linear-gradient(135deg, #a5b4fc, #818cf8)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
            }}
          >
            Society Portal
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Manage your society with ease
          </Typography>
        </Stack>

        <Paper
          elevation={3}
          sx={{
            p: 4,
            boxShadow: '0 8px 40px rgba(0,0,0,0.35), 0 0 0 1px rgba(129,140,248,0.15)',
          }}
        >
          <Typography variant="subtitle1" gutterBottom sx={{ mb: 0.5 }}>
            Welcome back
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Sign in with your registered phone number.
          </Typography>

          <Box component="form" onSubmit={handleSubmit} noValidate>
            <TextField
              label="Phone"
              type="text"
              fullWidth
              required
              autoFocus
              autoComplete="username"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              inputProps={{ inputMode: 'numeric' }}
              sx={{ mb: 2 }}
              disabled={loading}
            />

            <TextField
              label="Password"
              type="password"
              fullWidth
              required
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              sx={{ mb: 2 }}
              disabled={loading}
            />

            {error && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {error}
              </Alert>
            )}

            <Button
              type="submit"
              variant="contained"
              fullWidth
              size="large"
              disabled={loading || !phone || !password}
              sx={{ py: 1.25 }}
            >
              {loading ? <CircularProgress size={22} color="inherit" /> : 'Sign in'}
            </Button>
          </Box>
        </Paper>
      </Container>
    </Box>
  );
}
