import { Link as RouterLink } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Grid,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import PaymentIcon from '@mui/icons-material/Payment';
import HistoryIcon from '@mui/icons-material/History';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';

import { getMeApi } from '../../api/auth.js';

export default function MyProfile() {
  const { data: member, isLoading, isError, error } = useQuery({
    queryKey: ['me'],
    queryFn: getMeApi,
    staleTime: 5 * 60 * 1000,
  });

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (isError) {
    return (
      <Alert severity="error" sx={{ m: 2 }}>
        {error?.message || 'Failed to load profile'}
      </Alert>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={1}
        flexWrap="wrap"
        useFlexGap
        sx={{ mb: 2, maxWidth: 720 }}
      >
        <Button
          variant="outlined"
          startIcon={<ReceiptLongIcon />}
          component={RouterLink}
          to="/member/dues"
        >
          My Dues
        </Button>
        <Button
          variant="contained"
          startIcon={<PaymentIcon />}
          component={RouterLink}
          to="/member/submit-payment"
        >
          Submit Payment
        </Button>
        <Button
          variant="outlined"
          startIcon={<HistoryIcon />}
          component={RouterLink}
          to="/member/payments"
        >
          My Payments
        </Button>
        <Button
          variant="outlined"
          startIcon={<PictureAsPdfIcon />}
          component={RouterLink}
          to="/member/receipts"
        >
          My Receipts
        </Button>
      </Stack>
      <Paper elevation={2} sx={{ p: 3, maxWidth: 480 }}>
        <Typography variant="h6" gutterBottom>
          My Profile
        </Typography>
        <Grid container spacing={2}>
          <Grid item xs={4}>
            <Typography color="text.secondary">Name</Typography>
          </Grid>
          <Grid item xs={8}>
            <Typography>{member.fullName}</Typography>
          </Grid>

          <Grid item xs={4}>
            <Typography color="text.secondary">Phone</Typography>
          </Grid>
          <Grid item xs={8}>
            <Typography>{member.phone}</Typography>
          </Grid>

          <Grid item xs={4}>
            <Typography color="text.secondary">Email</Typography>
          </Grid>
          <Grid item xs={8}>
            <Typography>{member.email || '—'}</Typography>
          </Grid>

          <Grid item xs={4}>
            <Typography color="text.secondary">Role</Typography>
          </Grid>
          <Grid item xs={8}>
            <Chip
              label={member.role}
              color={member.role === 'ADMIN' ? 'primary' : 'default'}
              size="small"
            />
          </Grid>
        </Grid>
      </Paper>
    </Box>
  );
}
