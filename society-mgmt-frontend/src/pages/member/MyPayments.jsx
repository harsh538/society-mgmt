import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Chip,
  IconButton,
  Pagination,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import VisibilityIcon from '@mui/icons-material/Visibility';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import { useQuery } from '@tanstack/react-query';

import DataTable from '../../components/DataTable.jsx';
import { getMyPaymentsApi, openProofInNewTab } from '../../api/payments.js';
import { openReceiptPdfInNewTab } from '../../api/receipts.js';

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

const statusColor = (s) =>
  s === 'VERIFIED' ? 'success' : s === 'REJECTED' ? 'error' : 'warning';

export default function MyPayments() {
  const [page, setPage] = useState(0);

  const query = useQuery({
    queryKey: ['my-payments', page],
    queryFn: () => getMyPaymentsApi({ page, size: PAGE_SIZE }),
    keepPreviousData: true,
  });

  const rows = query.data?.content || [];
  const totalPages = query.data?.totalPages || 0;

  const columns = [
    { field: 'createdAt', headerName: 'Submitted', renderCell: (r) => fmtDate(r.createdAt) },
    { field: 'unitNumber', headerName: 'Unit', renderCell: (r) => r.unitNumber || '—' },
    { field: 'amount', headerName: 'Amount', renderCell: (r) => currency(r.amount) },
    { field: 'method', headerName: 'Method' },
    { field: 'utrReference', headerName: 'UTR', renderCell: (r) => r.utrReference || '—' },
    {
      field: 'status',
      headerName: 'Status',
      renderCell: (r) => (
        <Stack direction="column" spacing={0.5} alignItems="flex-end">
          <Chip size="small" label={r.status} color={statusColor(r.status)} />
          {r.status === 'REJECTED' && r.rejectionReason && (
            <Typography variant="caption" color="error" sx={{ maxWidth: 200 }}>
              {r.rejectionReason}
            </Typography>
          )}
        </Stack>
      ),
    },
    {
      field: 'links',
      headerName: 'Files',
      renderCell: (r) => (
        <Stack direction="row" spacing={0.5} justifyContent="flex-end">
          {r.proofFilePath && (
            <Tooltip title="View proof">
              <IconButton size="small" onClick={() => openProofInNewTab(r.id)}>
                <VisibilityIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
          {r.receiptId && (
            <Tooltip title="Download receipt">
              <IconButton size="small" onClick={() => openReceiptPdfInNewTab(r.receiptId)}>
                <PictureAsPdfIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
        </Stack>
      ),
    },
  ];

  return (
    <Box sx={{ p: { xs: 2, sm: 3 }, maxWidth: 1000, mx: 'auto' }}>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        justifyContent="space-between"
        alignItems={{ sm: 'center' }}
        spacing={1}
        sx={{ mb: 2 }}
      >
        <Typography variant="h5">My Payments</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          component={RouterLink}
          to="/member/submit-payment"
        >
          Submit Payment
        </Button>
      </Stack>

      {query.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {query.error?.response?.data?.message || 'Failed to load payments'}
        </Alert>
      )}

      <DataTable
        columns={columns}
        rows={rows}
        loading={query.isFetching}
        emptyText="No payments submitted yet."
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
    </Box>
  );
}
