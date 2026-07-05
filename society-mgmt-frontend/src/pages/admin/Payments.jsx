import { useState } from 'react';
import {
  Alert,
  Box,
  Chip,
  IconButton,
  MenuItem,
  Pagination,
  Stack,
  TextField,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material';
import VisibilityIcon from '@mui/icons-material/Visibility';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import { useQuery } from '@tanstack/react-query';

import DataTable from '../../components/DataTable.jsx';
import { getPaymentsApi, openProofInNewTab } from '../../api/payments.js';
import { openReceiptPdfInNewTab } from '../../api/receipts.js';

const PAGE_SIZE = 10;
const STATUSES = ['', 'PENDING', 'VERIFIED', 'REJECTED'];

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

export default function Payments() {
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState('');
  const [unitId, setUnitId] = useState('');

  const query = useQuery({
    queryKey: ['payments', { page, status, unitId }],
    queryFn: () =>
      getPaymentsApi({
        page,
        size: PAGE_SIZE,
        status: status || undefined,
        unitId: unitId || undefined,
      }),
    keepPreviousData: true,
  });

  const rows = query.data?.content || [];
  const totalPages = query.data?.totalPages || 0;

  const columns = [
    { field: 'id', headerName: 'ID' },
    { field: 'unitNumber', headerName: 'Unit', renderCell: (r) => r.unitNumber || '—' },
    { field: 'submittedByName', headerName: 'Member', renderCell: (r) => r.submittedByName || '—' },
    {
      field: 'amount',
      headerName: 'Amount',
      renderCell: (r) => currency(r.amount),
    },
    { field: 'paymentType', headerName: 'Type' },
    { field: 'method', headerName: 'Method' },
    {
      field: 'status',
      headerName: 'Status',
      renderCell: (r) => (
        <Chip size="small" label={r.status} color={statusColor(r.status)} />
      ),
    },
    {
      field: 'createdAt',
      headerName: 'Submitted',
      renderCell: (r) => fmtDate(r.createdAt),
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
            <Tooltip title="Download receipt PDF">
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
    <Box sx={{ p: { xs: 2, sm: 3 } }}>
      <Typography variant="h5" sx={{ mb: 2 }}>
        Payments
      </Typography>

      <Toolbar
        disableGutters
        sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.5, mb: 2 }}
      >
        <TextField
          label="Status"
          select
          size="small"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value);
            setPage(0);
          }}
          sx={{ minWidth: 160 }}
        >
          {STATUSES.map((s) => (
            <MenuItem key={s || 'all'} value={s}>
              {s || 'All'}
            </MenuItem>
          ))}
        </TextField>
        <TextField
          label="Unit ID"
          size="small"
          value={unitId}
          onChange={(e) => {
            setUnitId(e.target.value.replace(/\D/g, ''));
            setPage(0);
          }}
          sx={{ minWidth: 140 }}
        />
      </Toolbar>

      {query.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {query.error?.response?.data?.message || 'Failed to load payments'}
        </Alert>
      )}

      <DataTable
        columns={columns}
        rows={rows}
        loading={query.isFetching}
        emptyText="No payments found."
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
