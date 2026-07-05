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
  Snackbar,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import VisibilityIcon from '@mui/icons-material/Visibility';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import DataTable from '../../components/DataTable.jsx';
import {
  getPendingQueueApi,
  openProofInNewTab,
  rejectPaymentApi,
  verifyPaymentApi,
} from '../../api/payments.js';

const PAGE_SIZE = 10;

const currency = (v) => {
  if (v === null || v === undefined || v === '') return '₹0.00';
  const n = Number(v);
  if (Number.isNaN(n)) return String(v);
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(n);
};

const fmtDate = (iso) => {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleString('en-IN', {
      dateStyle: 'medium',
      timeStyle: 'short',
      timeZone: 'Asia/Kolkata',
    });
  } catch {
    return iso;
  }
};

export default function PaymentQueue() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [toast, setToast] = useState(null);   // {message, severity}
  const [rejectTarget, setRejectTarget] = useState(null);
  const [rejectionReason, setRejectionReason] = useState('');

  const query = useQuery({
    queryKey: ['payments', 'pending', page],
    queryFn: () => getPendingQueueApi({ page, size: PAGE_SIZE }),
    keepPreviousData: true,
    refetchInterval: 30000,
  });

  const rows = query.data?.content || [];
  const totalPages = query.data?.totalPages || 0;

  const verifyMutation = useMutation({
    mutationFn: verifyPaymentApi,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments', 'pending'] });
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      queryClient.invalidateQueries({ queryKey: ['receipts'] });
      setToast({ severity: 'success', message: 'Payment verified. Receipt generated.' });
    },
    onError: (err) =>
      setToast({
        severity: 'error',
        message: err?.response?.data?.message || 'Failed to verify payment',
      }),
  });

  const rejectMutation = useMutation({
    mutationFn: ({ id, body }) => rejectPaymentApi(id, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments', 'pending'] });
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      setRejectTarget(null);
      setRejectionReason('');
      setToast({ severity: 'success', message: 'Payment rejected.' });
    },
    onError: (err) =>
      setToast({
        severity: 'error',
        message: err?.response?.data?.message || 'Failed to reject payment',
      }),
  });

  const columns = [
    { field: 'createdAt', headerName: 'Submitted', renderCell: (r) => fmtDate(r.createdAt) },
    { field: 'unitNumber', headerName: 'Unit', renderCell: (r) => r.unitNumber || '—' },
    { field: 'submittedByName', headerName: 'Member', renderCell: (r) => r.submittedByName || '—' },
    {
      field: 'amount',
      headerName: 'Amount',
      renderCell: (r) => (
        <Typography component="span" sx={{ fontWeight: 600 }}>
          {currency(r.amount)}
        </Typography>
      ),
    },
    { field: 'method', headerName: 'Method', renderCell: (r) => r.method || '—' },
    { field: 'utrReference', headerName: 'UTR', renderCell: (r) => r.utrReference || '—' },
    {
      field: 'proof',
      headerName: 'Proof',
      renderCell: (r) =>
        r.proofFilePath ? (
          <Tooltip title="View proof">
            <IconButton size="small" onClick={() => openProofInNewTab(r.id)}>
              <VisibilityIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        ) : (
          <Typography variant="caption" color="text.secondary">none</Typography>
        ),
    },
    {
      field: 'actions',
      headerName: 'Actions',
      renderCell: (r) => (
        <Stack direction="row" spacing={0.5} justifyContent="flex-end">
          <Tooltip title="Verify">
            <span>
              <IconButton
                size="small"
                color="success"
                disabled={verifyMutation.isPending}
                onClick={() => verifyMutation.mutate(r.id)}
              >
                <CheckCircleIcon fontSize="small" />
              </IconButton>
            </span>
          </Tooltip>
          <Tooltip title="Reject">
            <IconButton
              size="small"
              color="error"
              onClick={() => {
                setRejectionReason('');
                setRejectTarget(r);
              }}
            >
              <CancelIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Stack>
      ),
    },
  ];

  return (
    <Box sx={{ p: { xs: 2, sm: 3 } }}>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        justifyContent="space-between"
        alignItems={{ sm: 'center' }}
        spacing={1}
        sx={{ mb: 2 }}
      >
        <Box>
          <Typography variant="h5">Payment Queue</Typography>
          <Typography variant="body2" color="text.secondary">
            Pending payments awaiting verification. Auto-refreshes every 30s.
          </Typography>
        </Box>
        <Chip label={`${query.data?.totalElements ?? 0} pending`} color="warning" />
      </Stack>

      {query.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {query.error?.response?.data?.message || 'Failed to load pending payments'}
        </Alert>
      )}

      <DataTable
        columns={columns}
        rows={rows}
        loading={query.isFetching}
        emptyText="No payments pending verification."
      />

      {totalPages > 1 && (
        <Box sx={{ mt: 2, display: 'flex', justifyContent: 'center' }}>
          <Pagination
            count={totalPages}
            page={page + 1}
            onChange={(_, p) => setPage(p - 1)}
            color="primary"
          />
        </Box>
      )}

      <Dialog
        open={!!rejectTarget}
        onClose={() => setRejectTarget(null)}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle>Reject payment</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {rejectTarget && (
              <Typography variant="body2" color="text.secondary">
                Rejecting {currency(rejectTarget.amount)} payment from{' '}
                <strong>{rejectTarget.submittedByName}</strong> for unit{' '}
                <strong>{rejectTarget.unitNumber}</strong>.
              </Typography>
            )}
            <TextField
              label="Reason"
              value={rejectionReason}
              onChange={(e) => setRejectionReason(e.target.value)}
              multiline
              rows={3}
              required
              autoFocus
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectTarget(null)} disabled={rejectMutation.isPending}>
            Cancel
          </Button>
          <Button
            variant="contained"
            color="error"
            disabled={!rejectionReason.trim() || rejectMutation.isPending}
            onClick={() =>
              rejectMutation.mutate({
                id: rejectTarget.id,
                body: { rejectionReason: rejectionReason.trim() },
              })
            }
          >
            Reject
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={!!toast}
        autoHideDuration={4000}
        onClose={() => setToast(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        {toast ? (
          <Alert severity={toast.severity} onClose={() => setToast(null)}>
            {toast.message}
          </Alert>
        ) : undefined}
      </Snackbar>
    </Box>
  );
}
