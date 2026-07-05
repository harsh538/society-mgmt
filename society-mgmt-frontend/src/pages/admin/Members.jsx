import { useEffect, useState } from 'react';
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
  Stack,
  TextField,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import KeyIcon from '@mui/icons-material/VpnKey';
import AddIcon from '@mui/icons-material/Add';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import DataTable from '../../components/DataTable.jsx';
import {
  activateMemberApi,
  createMemberApi,
  deleteMemberApi,
  getMembersApi,
  updateMemberApi,
} from '../../api/members.js';

const ROLES = ['MEMBER', 'ADMIN'];
const PAGE_SIZE = 10;

export default function Members() {
  const queryClient = useQueryClient();

  // ---- filters / pagination ----
  const [page, setPage] = useState(0);
  const [roleFilter, setRoleFilter] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');

  // Debounce search input (400ms).
  useEffect(() => {
    const t = setTimeout(() => {
      setSearch(searchInput.trim());
      setPage(0);
    }, 400);
    return () => clearTimeout(t);
  }, [searchInput]);

  const query = useQuery({
    queryKey: ['members', { page, role: roleFilter, search }],
    queryFn: () =>
      getMembersApi({
        page,
        size: PAGE_SIZE,
        role: roleFilter || undefined,
        search: search || undefined,
      }),
    keepPreviousData: true,
  });

  const rows = query.data?.content || [];
  const totalPages = query.data?.totalPages || 0;

  // ---- mutations ----
  const createMutation = useMutation({
    mutationFn: createMemberApi,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['members'] }),
  });
  const updateMutation = useMutation({
    mutationFn: ({ id, body }) => updateMemberApi(id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['members'] }),
  });
  const deleteMutation = useMutation({
    mutationFn: deleteMemberApi,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['members'] }),
  });
  const activateMutation = useMutation({
    mutationFn: ({ id, body }) => activateMemberApi(id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['members'] }),
  });

  // ---- dialog state ----
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState(null);    // member object or null
  const [activating, setActivating] = useState(null); // member object or null

  const handleDelete = (m) => {
    if (window.confirm(`Soft-delete member "${m.fullName}"?`)) {
      deleteMutation.mutate(m.id);
    }
  };

  const columns = [
    { field: 'fullName', headerName: 'Name' },
    { field: 'phone', headerName: 'Phone' },
    { field: 'email', headerName: 'Email', renderCell: (r) => r.email || '—' },
    {
      field: 'role',
      headerName: 'Role',
      renderCell: (r) => (
        <Chip
          size="small"
          label={r.role}
          color={r.role === 'ADMIN' ? 'secondary' : 'default'}
        />
      ),
    },
    {
      field: 'active',
      headerName: 'Status',
      renderCell: (r) => (
        <Chip
          size="small"
          label={r.active ? 'Active' : 'Inactive'}
          color={r.active ? 'success' : 'default'}
        />
      ),
    },
    {
      field: 'actions',
      headerName: 'Actions',
      renderCell: (r) => (
        <Stack direction="row" spacing={0.5} justifyContent="flex-end">
          <Tooltip title="Edit">
            <IconButton size="small" onClick={() => setEditing(r)}>
              <EditIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Set password">
            <IconButton size="small" onClick={() => setActivating(r)}>
              <KeyIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Delete">
            <IconButton size="small" color="error" onClick={() => handleDelete(r)}>
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Stack>
      ),
    },
  ];

  return (
    <Box sx={{ p: { xs: 2, sm: 3 } }}>
      <Typography variant="h5" sx={{ mb: 2 }}>Members</Typography>

      <Toolbar
        disableGutters
        sx={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: 1.5,
          mb: 2,
          alignItems: 'flex-start',
        }}
      >
        <TextField
          label="Search by name"
          size="small"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          sx={{ minWidth: 200 }}
        />
        <TextField
          label="Role"
          select
          size="small"
          value={roleFilter}
          onChange={(e) => {
            setRoleFilter(e.target.value);
            setPage(0);
          }}
          sx={{ minWidth: 140 }}
        >
          <MenuItem value="">All</MenuItem>
          {ROLES.map((r) => (
            <MenuItem key={r} value={r}>{r}</MenuItem>
          ))}
        </TextField>
        <Box sx={{ flexGrow: 1 }} />
        <Button startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
          Add Member
        </Button>
      </Toolbar>

      {query.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {query.error?.response?.data?.message || 'Failed to load members'}
        </Alert>
      )}

      <DataTable
        columns={columns}
        rows={rows}
        loading={query.isFetching}
        emptyText="No members yet — click Add Member."
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

      <CreateMemberDialog
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onSubmit={(body) =>
          createMutation.mutateAsync(body).then(() => setCreateOpen(false))
        }
        error={createMutation.error?.response?.data?.message}
        submitting={createMutation.isPending}
      />

      <EditMemberDialog
        member={editing}
        onClose={() => setEditing(null)}
        onSubmit={(body) =>
          updateMutation
            .mutateAsync({ id: editing.id, body })
            .then(() => setEditing(null))
        }
        error={updateMutation.error?.response?.data?.message}
        submitting={updateMutation.isPending}
      />

      <ActivateDialog
        member={activating}
        onClose={() => setActivating(null)}
        onSubmit={(body) =>
          activateMutation
            .mutateAsync({ id: activating.id, body })
            .then(() => setActivating(null))
        }
        error={activateMutation.error?.response?.data?.message}
        submitting={activateMutation.isPending}
      />
    </Box>
  );
}

function CreateMemberDialog({ open, onClose, onSubmit, error, submitting }) {
  const [form, setForm] = useState({ fullName: '', phone: '', email: '', role: 'MEMBER' });

  useEffect(() => {
    if (open) setForm({ fullName: '', phone: '', email: '', role: 'MEMBER' });
  }, [open]);

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Add Member</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField label="Full name" required value={form.fullName} onChange={set('fullName')} />
          <TextField label="Phone" required value={form.phone} onChange={set('phone')} />
          <TextField label="Email" value={form.email} onChange={set('email')} />
          <TextField label="Role" select value={form.role} onChange={set('role')}>
            {ROLES.map((r) => (
              <MenuItem key={r} value={r}>{r}</MenuItem>
            ))}
          </TextField>
          {error && <Alert severity="error">{error}</Alert>}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button variant="text" onClick={onClose} disabled={submitting}>Cancel</Button>
        <Button
          onClick={() => onSubmit(form)}
          disabled={submitting || !form.fullName || !form.phone}
        >
          Create
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function EditMemberDialog({ member, onClose, onSubmit, error, submitting }) {
  const [form, setForm] = useState({ fullName: '', phone: '', email: '', role: 'MEMBER' });

  useEffect(() => {
    if (member) {
      setForm({
        fullName: member.fullName || '',
        phone: member.phone || '',
        email: member.email || '',
        role: member.role || 'MEMBER',
      });
    }
  }, [member]);

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  return (
    <Dialog open={!!member} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Edit Member</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField label="Full name" value={form.fullName} onChange={set('fullName')} />
          <TextField label="Phone" value={form.phone} onChange={set('phone')} />
          <TextField label="Email" value={form.email} onChange={set('email')} />
          <TextField label="Role" select value={form.role} onChange={set('role')}>
            {ROLES.map((r) => (
              <MenuItem key={r} value={r}>{r}</MenuItem>
            ))}
          </TextField>
          {error && <Alert severity="error">{error}</Alert>}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button variant="text" onClick={onClose} disabled={submitting}>Cancel</Button>
        <Button onClick={() => onSubmit(form)} disabled={submitting}>Save</Button>
      </DialogActions>
    </Dialog>
  );
}

function ActivateDialog({ member, onClose, onSubmit, error, submitting }) {
  const [password, setPassword] = useState('');

  useEffect(() => { if (member) setPassword(''); }, [member]);

  return (
    <Dialog open={!!member} onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>Set password</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <Typography variant="body2" color="text.secondary">
            Set a new login password for <strong>{member?.fullName}</strong>. Minimum 6 characters.
          </Typography>
          <TextField
            label="New password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          {error && <Alert severity="error">{error}</Alert>}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button variant="text" onClick={onClose} disabled={submitting}>Cancel</Button>
        <Button
          onClick={() => onSubmit({ password })}
          disabled={submitting || password.length < 6}
        >
          Set Password
        </Button>
      </DialogActions>
    </Dialog>
  );
}
