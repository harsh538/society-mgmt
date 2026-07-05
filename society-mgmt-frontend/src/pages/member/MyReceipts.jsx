import { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  IconButton,
  Pagination,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import { useQuery } from '@tanstack/react-query';

import DataTable from '../../components/DataTable.jsx';
import { getReceiptsApi, openReceiptPdfInNewTab } from '../../api/receipts.js';

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

export default function MyReceipts() {
  const [page, setPage] = useState(0);

  const query = useQuery({
    queryKey: ['receipts', page],
    queryFn: () => getReceiptsApi({ page, size: PAGE_SIZE }),
    keepPreviousData: true,
  });

  const rows = query.data?.content || [];
  const totalPages = query.data?.totalPages || 0;

  const columns = [
    { field: 'receiptNumber', headerName: 'Receipt #' },
    { field: 'unitNumber', headerName: 'Unit', renderCell: (r) => r.unitNumber || '—' },
    { field: 'amount', headerName: 'Amount', renderCell: (r) => currency(r.amount) },
    { field: 'issuedAt', headerName: 'Issued', renderCell: (r) => fmtDate(r.issuedAt) },
    {
      field: 'pdf',
      headerName: 'PDF',
      renderCell: (r) => (
        <Stack direction="row" justifyContent="flex-end">
          <Tooltip title="Download PDF">
            <span>
              <Button
                size="small"
                startIcon={<PictureAsPdfIcon />}
                onClick={() => openReceiptPdfInNewTab(r.id)}
                disabled={!r.pdfFilePath}
              >
                Open
              </Button>
            </span>
          </Tooltip>
        </Stack>
      ),
    },
  ];

  return (
    <Box sx={{ p: { xs: 2, sm: 3 }, maxWidth: 960, mx: 'auto' }}>
      <Typography variant="h5" sx={{ mb: 2 }}>
        My Receipts
      </Typography>

      {query.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {query.error?.response?.data?.message || 'Failed to load receipts'}
        </Alert>
      )}

      <DataTable
        columns={columns}
        rows={rows}
        loading={query.isFetching}
        emptyText="No receipts yet."
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
