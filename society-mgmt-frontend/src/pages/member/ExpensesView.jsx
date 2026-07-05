import { useState } from 'react';
import {
  Alert,
  Box,
  Chip,
  FormControl,
  InputLabel,
  MenuItem,
  Pagination,
  Select,
  Stack,
  Toolbar,
  Typography,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';

import DataTable from '../../components/DataTable.jsx';
import { getExpensesApi, getExpenseSummaryApi } from '../../api/expenses.js';

const PAGE_SIZE = 15;

const CATEGORIES = [
  'GARBAGE', 'ELECTRICITY', 'LIFT', 'WATER',
  'SECURITY', 'REPAIRS', 'ELECTRICAL_GOODS', 'OTHER',
];

const currency = (v) => {
  const n = Number(v);
  if (!v || Number.isNaN(n)) return '₹0.00';
  return new Intl.NumberFormat('en-IN', {
    style: 'currency', currency: 'INR', maximumFractionDigits: 2,
  }).format(n);
};

const categoryColor = {
  GARBAGE: 'default', ELECTRICITY: 'warning', LIFT: 'info',
  WATER: 'primary', SECURITY: 'secondary', REPAIRS: 'error',
  ELECTRICAL_GOODS: 'warning', OTHER: 'default',
};

export default function ExpensesView() {
  const [page, setPage] = useState(0);
  const [filterCat, setFilterCat] = useState('');

  const listQuery = useQuery({
    queryKey: ['expenses-member', { page, filterCat }],
    queryFn: () => getExpensesApi({
      page, size: PAGE_SIZE,
      category: filterCat || undefined,
    }),
    keepPreviousData: true,
  });

  const summaryQuery = useQuery({
    queryKey: ['expenses-summary-member'],
    queryFn: () => getExpenseSummaryApi({}),
  });

  const rows = listQuery.data?.content || [];
  const totalPages = listQuery.data?.totalPages || 0;
  const summary = summaryQuery.data;

  const columns = [
    { field: 'expenseDate', headerName: 'Date', renderCell: (r) => r.expenseDate || '—' },
    {
      field: 'category', headerName: 'Category',
      renderCell: (r) => (
        <Chip size="small" label={r.category} color={categoryColor[r.category] || 'default'} />
      ),
    },
    { field: 'title', headerName: 'Description' },
    { field: 'vendorName', headerName: 'Vendor', renderCell: (r) => r.vendorName || '—' },
    { field: 'amount', headerName: 'Amount', renderCell: (r) => currency(r.amount) },
    { field: 'paidFrom', headerName: 'Paid From', renderCell: (r) => r.paidFrom || '—' },
    { field: 'note', headerName: 'Note', renderCell: (r) => r.note || '—' },
  ];

  return (
    <Box sx={{ p: { xs: 2, sm: 3 } }}>
      <Typography variant="h5" sx={{ mb: 2 }}>Society Expenses</Typography>

      {summary && (
        <Box sx={{ mb: 2, p: 2, bgcolor: 'background.paper', borderRadius: 1, border: '1px solid', borderColor: 'divider' }}>
          <Typography variant="subtitle2" color="text.secondary">Total Expenses (All Time)</Typography>
          <Typography variant="h6">{currency(summary.grandTotal)}</Typography>
          {summary.byCategory?.length > 0 && (
            <Stack direction="row" flexWrap="wrap" gap={1} sx={{ mt: 1 }}>
              {summary.byCategory.map((c) => (
                <Chip key={c.category} size="small"
                  label={`${c.category}: ${currency(c.total)}`}
                  color={categoryColor[c.category] || 'default'}
                />
              ))}
            </Stack>
          )}
        </Box>
      )}

      <Toolbar disableGutters sx={{ mb: 2, flexWrap: 'wrap', gap: 1.5 }}>
        <FormControl size="small" sx={{ minWidth: 180 }}>
          <InputLabel>Category</InputLabel>
          <Select value={filterCat} label="Category"
            onChange={(e) => { setFilterCat(e.target.value); setPage(0); }}>
            <MenuItem value="">All</MenuItem>
            {CATEGORIES.map((c) => <MenuItem key={c} value={c}>{c}</MenuItem>)}
          </Select>
        </FormControl>
      </Toolbar>

      {listQuery.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {listQuery.error?.response?.data?.message || 'Failed to load expenses'}
        </Alert>
      )}

      <DataTable columns={columns} rows={rows} loading={listQuery.isFetching}
        emptyText="No expenses recorded yet." />

      {totalPages > 1 && (
        <Box sx={{ mt: 2, display: 'flex', justifyContent: 'center' }}>
          <Pagination count={totalPages} page={page + 1} onChange={(_, p) => setPage(p - 1)} color="primary" />
        </Box>
      )}
    </Box>
  );
}
