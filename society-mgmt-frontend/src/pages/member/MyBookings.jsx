import { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Pagination,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import CancelIcon from '@mui/icons-material/Cancel';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import DataTable from '../../components/DataTable.jsx';
import { cancelBookingApi, getMyBookingsApi, requestBookingApi } from '../../api/bookings.js';

const PAGE_SIZE = 10;

const statusColor = {
  REQUESTED: 'warning', APPROVED: 'success', REJECTED: 'error', CANCELLED: 'default',
};

const currency = (v) => {
  const n = Number(v);
  if (!v || Number.isNaN(n)) return '₹0.00';
  return new Intl.NumberFormat('en-IN', {
    style: 'currency', currency: 'INR', maximumFractionDigits: 2,
  }).format(n);
};

const today = new Date().toISOString().split('T')[0];

export default function MyBookings() {
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState({ eventTitle: '', eventDate: '' });
  const [formErr, setFormErr] = useState('');

  const listQuery = useQuery({
    queryKey: ['bookings-mine', { page }],
    queryFn: () => getMyBookingsApi({ page, size: PAGE_SIZE }),
    keepPreviousData: true,
  });

  const requestMut = useMutation({
    mutationFn: (data) => requestBookingApi(data),
    onSuccess: () => { qc.invalidateQueries(['bookings-mine']); setDialogOpen(false); setForm({ eventTitle: '', eventDate: '' }); setFormErr(''); },
    onError: (e) => setFormErr(e?.response?.data?.message || 'Failed to submit booking'),
  });

  const cancelMut = useMutation({
    mutationFn: (id) => cancelBookingApi(id),
    onSuccess: () => qc.invalidateQueries(['bookings-mine']),
  });

  const rows = listQuery.data?.content || [];
  const totalPages = listQuery.data?.totalPages || 0;

  function handleSubmit() {
    if (!form.eventTitle || !form.eventDate) {
      setFormErr('Event title and date are required.');
      return;
    }
    requestMut.mutate({ eventTitle: form.eventTitle, eventDate: form.eventDate });
  }

  const columns = [
    { field: 'id', headerName: 'ID' },
    { field: 'eventTitle', headerName: 'Event' },
    { field: 'eventDate', headerName: 'Date', renderCell: (r) => r.eventDate || '—' },
    { field: 'nominalFee', headerName: 'Fee', renderCell: (r) => currency(r.nominalFee) },
    {
      field: 'status', headerName: 'Status',
      renderCell: (r) => (
        <Chip size="small" label={r.status} color={statusColor[r.status] || 'default'} />
      ),
    },
    {
      field: 'actions', headerName: '',
      renderCell: (r) => r.status === 'REQUESTED' ? (
        <Tooltip title="Cancel booking">
          <IconButton size="small" onClick={() => cancelMut.mutate(r.id)}>
            <CancelIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      ) : null,
    },
  ];

  return (
    <Box sx={{ p: { xs: 2, sm: 3 } }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
        <Typography variant="h5">My Terrace Bookings</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => { setDialogOpen(true); setFormErr(''); }}>
          Request Booking
        </Button>
      </Stack>

      {listQuery.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {listQuery.error?.response?.data?.message || 'Failed to load bookings'}
        </Alert>
      )}

      <DataTable columns={columns} rows={rows} loading={listQuery.isFetching}
        emptyText="No terrace bookings yet." />

      {totalPages > 1 && (
        <Box sx={{ mt: 2, display: 'flex', justifyContent: 'center' }}>
          <Pagination count={totalPages} page={page + 1} onChange={(_, p) => setPage(p - 1)} color="primary" />
        </Box>
      )}

      {/* Request booking dialog */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Request Terrace Booking</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {formErr && <Alert severity="error">{formErr}</Alert>}
            <Typography variant="body2" color="text.secondary">
              Submit a request to book the terrace for your event. The admin will review and approve it.
            </Typography>
            <TextField label="Event Title" size="small" required value={form.eventTitle}
              onChange={(e) => setForm(f => ({ ...f, eventTitle: e.target.value }))} />
            <TextField label="Event Date" size="small" type="date" required value={form.eventDate}
              InputLabelProps={{ shrink: true }}
              inputProps={{ min: today }}
              onChange={(e) => setForm(f => ({ ...f, eventDate: e.target.value }))} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit} disabled={requestMut.isLoading}>
            {requestMut.isLoading ? 'Submitting…' : 'Submit Request'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
