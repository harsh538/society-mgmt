import { useMemo, useState } from 'react';
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
  MenuItem,
  Pagination,
  Paper,
  Stack,
  TextField,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import BlockIcon from '@mui/icons-material/Block';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import DataTable from '../../components/DataTable.jsx';
import {
  generateChargesApi,
  getChargesApi,
  voidChargeApi,
} from '../../api/charges.js';

const MONTHS = [
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
];

const STATUSES = ['DUE', 'PARTIAL', 'PAID', 'VOID'];
const PAGE_SIZE = 25;

const currency = (v) => {
  if (v === null || v === undefined || v === '') return '—';
  const n = Number(v);
  if (Number.isNaN(n)) return String(v);
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(n);
};

const statusColor = (s) =>
  s === 'PAID' ? 'success'
    : s === 'PARTIAL' ? 'warning'
    : s === 'VOID' ? 'default'
    : 'error';

export default function GenerateCharges() {
  const queryClient = useQueryClient();
  const now = new Date();

  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [voiding, setVoiding] = useState(null);   // charge object being voided
  const [voidReason, setVoidReason] = useState('');
  const [genResult, setGenResult] = useState(null);

  // Year picker: current ± 5
  const yearOptions = useMemo(() => {
    const y = now.getFullYear();
    const arr = [];
    for (let i = y - 5; i <= y + 1; i += 1) arr.push(i);
    return arr;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ---- list ----
  const listQuery = useQuery({
    queryKey: ['charges', { year, month, statusFilter, page }],
    queryFn: () =>
      getChargesApi({
        year,
        month,
        status: statusFilter || undefined,
        page,
        size: PAGE_SIZE,
      }),
    keepPreviousData: true,
  });

  const rows = listQuery.data?.content || [];
  const totalPages = listQuery.data?.totalPages || 0;

  // ---- generate ----
  const generateMutation = useMutation({
    mutationFn: generateChargesApi,
    onSuccess: (envelope) => {
      setGenResult({
        message: envelope?.message,
        created: envelope?.data?.created ?? 0,
        skipped: envelope?.data?.skipped ?? 0,
        total: envelope?.data?.total ?? 0,
      });
      queryClient.invalidateQueries({ queryKey: ['charges'] });
    },
    onError: () => setGenResult(null),
  });

  const handleGenerate = () => {
    setGenResult(null);
    generateMutation.mutate({ year, month });
  };

  // ---- void ----
  const voidMutation = useMutation({
    mutationFn: ({ id, reason }) => voidChargeApi(id, { reason }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['charges'] });
      setVoiding(null);
      setVoidReason('');
    },
  });

  const columns = [
    { field: 'unitNumber', headerName: 'Unit #' },
    { field: 'unitType', headerName: 'Type' },
    {
      field: 'period',
      headerName: 'Period',
      renderCell: (r) => `${MONTHS[r.periodMonth - 1]} ${r.periodYear}`,
    },
    {
      field: 'amountDue',
      headerName: 'Due (₹)',
      renderCell: (r) => currency(r.amountDue),
    },
    {
      field: 'amountPaid',
      headerName: 'Paid (₹)',
      renderCell: (r) => currency(r.amountPaid),
    },
    {
      field: 'outstanding',
      headerName: 'Outstanding (₹)',
      renderCell: (r) => currency(r.outstanding),
    },
    {
      field: 'status',
      headerName: 'Status',
      renderCell: (r) => (
        <Chip size="small" label={r.status} color={statusColor(r.status)} />
      ),
    },
    {
      field: 'actions',
      headerName: 'Actions',
      renderCell: (r) => (
        <Stack direction="row" spacing={0.5} justifyContent="flex-end">
          {(r.status === 'DUE' || r.status === 'PARTIAL') ? (
            <Tooltip title="Void charge">
              <IconButton
                size="small"
                color="error"
                onClick={() => { setVoiding(r); setVoidReason(''); }}
              >
                <BlockIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          ) : (
            <Box sx={{ width: 32 }} />
          )}
        </Stack>
      ),
    },
  ];

  return (
    <Box sx={{ p: { xs: 2, sm: 3 }, maxWidth: 1200, mx: 'auto' }}>
      <Typography variant="h5" sx={{ mb: 2 }}>Generate Maintenance Charges</Typography>

      <Paper variant="outlined" sx={{ p: { xs: 2, sm: 3 }, mb: 3 }}>
        <Toolbar
          disableGutters
          sx={{
            display: 'flex',
            flexWrap: 'wrap',
            gap: 1.5,
            alignItems: 'flex-start',
          }}
        >
          <TextField
            label="Year"
            select
            size="small"
            value={year}
            onChange={(e) => { setYear(Number(e.target.value)); setPage(0); }}
            sx={{ minWidth: 120 }}
          >
            {yearOptions.map((y) => (
              <MenuItem key={y} value={y}>{y}</MenuItem>
            ))}
          </TextField>
          <TextField
            label="Month"
            select
            size="small"
            value={month}
            onChange={(e) => { setMonth(Number(e.target.value)); setPage(0); }}
            sx={{ minWidth: 140 }}
          >
            {MONTHS.map((m, idx) => (
              <MenuItem key={m} value={idx + 1}>{m}</MenuItem>
            ))}
          </TextField>
          <Box sx={{ flexGrow: 1 }} />
          <Button
            startIcon={<PlayArrowIcon />}
            variant="contained"
            onClick={handleGenerate}
            disabled={generateMutation.isPending}
          >
            Generate
          </Button>
        </Toolbar>

        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1.5 }}>
          Idempotent — runs only create missing charges. Active FLAT and SHOP
          units are billed (TERRACE excluded). Tenant-occupied units include the
          configured surcharge.
        </Typography>

        {generateMutation.isError && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {generateMutation.error?.response?.data?.message
              || 'Failed to generate charges'}
          </Alert>
        )}
        {genResult && (
          <Alert severity="success" sx={{ mt: 2 }}>
            {`Created: ${genResult.created}, Skipped: ${genResult.skipped} (already existed), Total active units: ${genResult.total}`}
          </Alert>
        )}
      </Paper>

      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={1}
        alignItems={{ sm: 'center' }}
        justifyContent="space-between"
        sx={{ mb: 2 }}
      >
        <Typography variant="h6">
          {`Charges — ${MONTHS[month - 1]} ${year}`}
        </Typography>
        <TextField
          label="Status filter"
          select
          size="small"
          value={statusFilter}
          onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
          sx={{ minWidth: 160 }}
        >
          <MenuItem value="">All (excl. void)</MenuItem>
          {STATUSES.map((s) => (
            <MenuItem key={s} value={s}>{s}</MenuItem>
          ))}
        </TextField>
      </Stack>

      {listQuery.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {listQuery.error?.response?.data?.message || 'Failed to load charges'}
        </Alert>
      )}

      <DataTable
        columns={columns}
        rows={rows}
        loading={listQuery.isFetching}
        emptyText="No charges for this period — click Generate."
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

      <Dialog open={!!voiding} onClose={() => setVoiding(null)} fullWidth maxWidth="sm">
        <DialogTitle>Void charge</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Typography variant="body2" color="text.secondary">
              {voiding
                ? `Unit ${voiding.unitNumber} — ${MONTHS[voiding.periodMonth - 1]} ${voiding.periodYear} (₹${voiding.amountDue})`
                : ''}
            </Typography>
            <TextField
              label="Reason (required)"
              value={voidReason}
              onChange={(e) => setVoidReason(e.target.value)}
              multiline
              minRows={2}
              required
            />
            {voidMutation.isError && (
              <Alert severity="error">
                {voidMutation.error?.response?.data?.message || 'Failed to void charge'}
              </Alert>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setVoiding(null)} disabled={voidMutation.isPending}>
            Cancel
          </Button>
          <Button
            color="error"
            variant="contained"
            disabled={!voidReason.trim() || voidMutation.isPending}
            onClick={() => voidMutation.mutate({ id: voiding.id, reason: voidReason.trim() })}
          >
            Void
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
