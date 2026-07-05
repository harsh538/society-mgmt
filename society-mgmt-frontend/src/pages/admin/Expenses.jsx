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
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import DataTable from '../../components/DataTable.jsx';
import {
  createExpenseApi,
  deleteExpenseApi,
  getExpensesApi,
  getExpenseSummaryApi,
  updateExpenseApi,
} from '../../api/expenses.js';

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

const EMPTY_FORM = {
  category: '', title: '', vendorName: '', amount: '',
  expenseDate: '', paidFrom: '', note: '',
};

export default function Expenses() {
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [filterCat, setFilterCat] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editTarget, setEditTarget] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [billFile, setBillFile] = useState(null);
  const [deleteId, setDeleteId] = useState(null);
  const [formErr, setFormErr] = useState('');

  const listQuery = useQuery({
    queryKey: ['expenses', { page, filterCat }],
    queryFn: () => getExpensesApi({
      page, size: PAGE_SIZE,
      category: filterCat || undefined,
    }),
    keepPreviousData: true,
  });

  const summaryQuery = useQuery({
    queryKey: ['expenses-summary'],
    queryFn: () => getExpenseSummaryApi({}),
  });

  const createMut = useMutation({
    mutationFn: ({ expense, file }) => createExpenseApi(expense, file),
    onSuccess: () => { qc.invalidateQueries(['expenses']); qc.invalidateQueries(['expenses-summary']); closeDialog(); },
    onError: (e) => setFormErr(e?.response?.data?.message || 'Failed to save expense'),
  });

  const updateMut = useMutation({
    mutationFn: ({ id, expense, file }) => updateExpenseApi(id, expense, file),
    onSuccess: () => { qc.invalidateQueries(['expenses']); qc.invalidateQueries(['expenses-summary']); closeDialog(); },
    onError: (e) => setFormErr(e?.response?.data?.message || 'Failed to update expense'),
  });

  const deleteMut = useMutation({
    mutationFn: (id) => deleteExpenseApi(id),
    onSuccess: () => { qc.invalidateQueries(['expenses']); qc.invalidateQueries(['expenses-summary']); setDeleteId(null); },
  });

  const rows = listQuery.data?.content || [];
  const totalPages = listQuery.data?.totalPages || 0;
  const summary = summaryQuery.data;

  function openCreate() {
    setEditTarget(null);
    setForm(EMPTY_FORM);
    setBillFile(null);
    setFormErr('');
    setDialogOpen(true);
  }

  function openEdit(row) {
    setEditTarget(row);
    setForm({
      category: row.category || '',
      title: row.title || '',
      vendorName: row.vendorName || '',
      amount: row.amount ?? '',
      expenseDate: row.expenseDate || '',
      paidFrom: row.paidFrom || '',
      note: row.note || '',
    });
    setBillFile(null);
    setFormErr('');
    setDialogOpen(true);
  }

  function closeDialog() {
    setDialogOpen(false);
    setEditTarget(null);
    setForm(EMPTY_FORM);
    setBillFile(null);
    setFormErr('');
  }

  function handleSubmit() {
    if (!form.category || !form.title || !form.amount || !form.expenseDate) {
      setFormErr('Category, title, amount and date are required.');
      return;
    }
    const expense = {
      category: form.category,
      title: form.title,
      vendorName: form.vendorName || null,
      amount: parseFloat(form.amount),
      expenseDate: form.expenseDate,
      paidFrom: form.paidFrom || null,
      note: form.note || null,
    };
    if (editTarget) {
      updateMut.mutate({ id: editTarget.id, expense, file: billFile });
    } else {
      createMut.mutate({ expense, file: billFile });
    }
  }

  const isSaving = createMut.isLoading || updateMut.isLoading;

  const columns = [
    { field: 'expenseDate', headerName: 'Date', renderCell: (r) => r.expenseDate || '—' },
    {
      field: 'category', headerName: 'Category',
      renderCell: (r) => (
        <Chip size="small" label={r.category} color={categoryColor[r.category] || 'default'} />
      ),
    },
    { field: 'title', headerName: 'Title' },
    { field: 'vendorName', headerName: 'Vendor', renderCell: (r) => r.vendorName || '—' },
    { field: 'amount', headerName: 'Amount', renderCell: (r) => currency(r.amount) },
    { field: 'paidFrom', headerName: 'Paid From', renderCell: (r) => r.paidFrom || '—' },
    { field: 'recordedByName', headerName: 'Recorded By', renderCell: (r) => r.recordedByName || '—' },
    {
      field: 'actions', headerName: '',
      renderCell: (r) => (
        <Stack direction="row" spacing={0.5} justifyContent="flex-end">
          <Tooltip title="Edit">
            <IconButton size="small" onClick={() => openEdit(r)}><EditIcon fontSize="small" /></IconButton>
          </Tooltip>
          <Tooltip title="Delete">
            <IconButton size="small" color="error" onClick={() => setDeleteId(r.id)}>
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Stack>
      ),
    },
  ];

  return (
    <Box sx={{ p: { xs: 2, sm: 3 } }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
        <Typography variant="h5">Society Expenses</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
          Add Expense
        </Button>
      </Stack>

      {summary && (
        <Box sx={{ mb: 2, p: 2, bgcolor: 'background.paper', borderRadius: 1, border: '1px solid', borderColor: 'divider' }}>
          <Typography variant="subtitle2" color="text.secondary">Total Expenses</Typography>
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
          <Select value={filterCat} label="Category" onChange={(e) => { setFilterCat(e.target.value); setPage(0); }}>
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

      <DataTable columns={columns} rows={rows} loading={listQuery.isFetching} emptyText="No expenses recorded." />

      {totalPages > 1 && (
        <Box sx={{ mt: 2, display: 'flex', justifyContent: 'center' }}>
          <Pagination count={totalPages} page={page + 1} onChange={(_, p) => setPage(p - 1)} color="primary" />
        </Box>
      )}

      {/* Create / Edit Dialog */}
      <Dialog open={dialogOpen} onClose={closeDialog} maxWidth="sm" fullWidth>
        <DialogTitle>{editTarget ? 'Edit Expense' : 'Add Expense'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {formErr && <Alert severity="error">{formErr}</Alert>}
            <FormControl fullWidth size="small" required>
              <InputLabel>Category</InputLabel>
              <Select value={form.category} label="Category"
                onChange={(e) => setForm(f => ({ ...f, category: e.target.value }))}>
                {CATEGORIES.map((c) => <MenuItem key={c} value={c}>{c}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField label="Title" size="small" required value={form.title}
              onChange={(e) => setForm(f => ({ ...f, title: e.target.value }))} />
            <TextField label="Vendor Name" size="small" value={form.vendorName}
              onChange={(e) => setForm(f => ({ ...f, vendorName: e.target.value }))} />
            <TextField label="Amount (₹)" size="small" type="number" required value={form.amount}
              inputProps={{ min: 0.01, step: '0.01' }}
              onChange={(e) => setForm(f => ({ ...f, amount: e.target.value }))} />
            <TextField label="Expense Date" size="small" type="date" required value={form.expenseDate}
              InputLabelProps={{ shrink: true }}
              onChange={(e) => setForm(f => ({ ...f, expenseDate: e.target.value }))} />
            <TextField label="Paid From" size="small" value={form.paidFrom}
              onChange={(e) => setForm(f => ({ ...f, paidFrom: e.target.value }))} />
            <TextField label="Note" size="small" multiline rows={2} value={form.note}
              onChange={(e) => setForm(f => ({ ...f, note: e.target.value }))} />
            <Box>
              <Typography variant="caption" color="text.secondary">
                {editTarget ? 'Replace bill (optional)' : 'Upload bill (optional)'}
              </Typography>
              <input type="file" accept="image/*,application/pdf" style={{ display: 'block', marginTop: 4 }}
                onChange={(e) => setBillFile(e.target.files?.[0] || null)} />
            </Box>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeDialog}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit} disabled={isSaving}>
            {isSaving ? 'Saving…' : editTarget ? 'Update' : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete confirm dialog */}
      <Dialog open={deleteId !== null} onClose={() => setDeleteId(null)}>
        <DialogTitle>Delete Expense</DialogTitle>
        <DialogContent>
          <Typography>Are you sure you want to delete this expense? This cannot be undone.</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteId(null)}>Cancel</Button>
          <Button variant="contained" color="error"
            onClick={() => deleteMut.mutate(deleteId)}
            disabled={deleteMut.isLoading}>
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
