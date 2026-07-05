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
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Pagination,
  Select,
  Stack,
  TextField,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import CancelIcon from '@mui/icons-material/Cancel';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import DataTable from '../../components/DataTable.jsx';
import {
  approveBookingApi,
  cancelBookingApi,
  getBookingsApi,
  rejectBookingApi,
} from '../../api/bookings.js';

const PAGE_SIZE = 15;
const STATUSES = ['', 'REQUESTED', 'APPROVED', 'REJECTED', 'CANCELLED'];

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

export default function Bookings() {
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [filterStatus, setFilterStatus] = useState('');

  const [approveOpen, setApproveOpen] = useState(false);
  const [approveTarget, setApproveTarget] = useState(null);
  const [nominalFee, setNominalFee] = useState('0');

  const [rejectOpen, setRejectOpen] = useState(false);
  const [rejectTarget, setRejectTarget] = useState(null);
  const [rejectReason, setRejectReason] = useState('');

  const [actionErr, setActionErr] = useState('');

  const listQuery = useQuery({
    queryKey: ['bookings-admin', { page, filterStatus }],
    queryFn: () => getBookingsApi({
      page, size: PAGE_SIZE,
      status: filterStatus || undefined,
    }),
    keepPreviousData: true,
  });

  const approveMut = useMutation({
    mutationFn: ({ id, nominalFee: fee }) => approveBookingApi(id, { nominalFee: parseFloat(fee) }),
    onSuccess: () => { qc.invalidateQueries(['bookings-admin']); setApproveOpen(false); setActionErr(''); },
    onError: (e) => setActionErr(e?.response?.data?.message || 'Approval failed'),
  });

  const rejectMut = useMutation({
    mutationFn: ({ id, reason }) => rejectBookingApi(id, { reason }),
    onSuccess: () => { qc.invalidateQueries(['bookings-admin']); setRejectOpen(false); setActionErr(''); },
    onError: (e) => setActionErr(e?.response?.data?.message || 'Rejection failed'),
  });

  const cancelMut = useMutation({
    mutationFn: (id) => cancelBookingApi(id),
    onSuccess: () => qc.invalidateQueries(['bookings-admin']),
  });

  const rows = listQuery.data?.content || [];
  const totalPages = listQuery.data?.totalPages || 0;

  const columns = [
    { field: 'id', headerName: 'ID' },
    { field: 'eventDate', headerName: 'Date', renderCell: (r) => r.eventDate || '—' },
    { field: 'eventTitle', headerName: 'Event' },
    { field: 'bookedByName', headerName: 'Requested By', renderCell: (r) => r.bookedByName || '—' },
    { field: 'unitNumber', headerName: 'Unit', renderCell: (r) => r.unitNumber || '—' },
    { field: 'nominalFee', headerName: 'Fee', renderCell: (r) => currency(r.nominalFee) },
    {
      field: 'status', headerName: 'Status',
      renderCell: (r) => (
        <Chip size="small" label={r.status} color={statusColor[r.status] || 'default'} />
      ),
    },
    { field: 'approvedByName', headerName: 'Actioned By', renderCell: (r) => r.approvedByName || '—' },
    {
      field: 'actions', headerName: '',
      renderCell: (r) => (
        <Stack direction="row" spacing={0.5} justifyContent="flex-end">
          {r.status === 'REQUESTED' && (
            <>
              <Tooltip title="Approve">
                <IconButton size="small" color="success" onClick={() => {
                  setApproveTarget(r); setNominalFee('0'); setActionErr(''); setApproveOpen(true);
                }}>
                  <CheckIcon fontSize="small" />
                </IconButton>
              </Tooltip>
              <Tooltip title="Reject">
                <IconButton size="small" color="error" onClick={() => {
                  setRejectTarget(r); setRejectReason(''); setActionErr(''); setRejectOpen(true);
                }}>
                  <CloseIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            </>
          )}
          {(r.status === 'REQUESTED' || r.status === 'APPROVED') && (
            <Tooltip title="Cancel">
              <IconButton size="small" onClick={() => cancelMut.mutate(r.id)}>
                <CancelIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
        </Stack>
      ),
    },
  ];

  return (
    <Box sx={{ p: { xs: 2, sm: 3 } }}>
      <Typography variant="h5" sx={{ mb: 2 }}>Terrace Bookings</Typography>

      <Toolbar disableGutters sx={{ mb: 2, flexWrap: 'wrap', gap: 1.5 }}>
        <FormControl size="small" sx={{ minWidth: 180 }}>
          <InputLabel>Status</InputLabel>
          <Select value={filterStatus} label="Status"
            onChange={(e) => { setFilterStatus(e.target.value); setPage(0); }}>
            {STATUSES.map((s) => <MenuItem key={s || 'all'} value={s}>{s || 'All'}</MenuItem>)}
          </Select>
        </FormControl>
      </Toolbar>

      {listQuery.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {listQuery.error?.response?.data?.message || 'Failed to load bookings'}
        </Alert>
      )}

      <DataTable columns={columns} rows={rows} loading={listQuery.isFetching}
        emptyText="No bookings found." />

      {totalPages > 1 && (
        <Box sx={{ mt: 2, display: 'flex', justifyContent: 'center' }}>
          <Pagination count={totalPages} page={page + 1} onChange={(_, p) => setPage(p - 1)} color="primary" />
        </Box>
      )}

      {/* Approve dialog */}
      <Dialog open={approveOpen} onClose={() => setApproveOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Approve Booking</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {actionErr && <Alert severity="error">{actionErr}</Alert>}
            {approveTarget && (
              <Typography variant="body2">
                <strong>{approveTarget.eventTitle}</strong> on {approveTarget.eventDate} by {approveTarget.bookedByName}
              </Typography>
            )}
            <TextField label="Nominal Fee (₹)" size="small" type="number" value={nominalFee}
              inputProps={{ min: 0, step: '0.01' }}
              onChange={(e) => setNominalFee(e.target.value)} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setApproveOpen(false)}>Cancel</Button>
          <Button variant="contained" color="success"
            onClick={() => approveMut.mutate({ id: approveTarget.id, nominalFee })}
            disabled={approveMut.isLoading}>
            Approve
          </Button>
        </DialogActions>
      </Dialog>

      {/* Reject dialog */}
      <Dialog open={rejectOpen} onClose={() => setRejectOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Reject Booking</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {actionErr && <Alert severity="error">{actionErr}</Alert>}
            {rejectTarget && (
              <Typography variant="body2">
                <strong>{rejectTarget.eventTitle}</strong> on {rejectTarget.eventDate}
              </Typography>
            )}
            <TextField label="Reason (optional)" size="small" multiline rows={2}
              value={rejectReason} onChange={(e) => setRejectReason(e.target.value)} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectOpen(false)}>Cancel</Button>
          <Button variant="contained" color="error"
            onClick={() => rejectMut.mutate({ id: rejectTarget.id, reason: rejectReason })}
            disabled={rejectMut.isLoading}>
            Reject
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
