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
  Divider,
  IconButton,
  List,
  ListItem,
  ListItemText,
  MenuItem,
  Pagination,
  Stack,
  TextField,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import PeopleIcon from '@mui/icons-material/People';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import DataTable from '../../components/DataTable.jsx';
import {
  createUnitApi,
  deleteUnitApi,
  getUnitApi,
  getUnitsApi,
  linkMemberApi,
  unlinkMemberApi,
  updateUnitApi,
} from '../../api/units.js';

const TYPES = ['FLAT', 'SHOP', 'TERRACE'];
const OCCUPANCIES = ['OWNER', 'TENANT', 'VACANT'];
const RELATIONSHIPS = ['OWNER', 'CO_OWNER', 'TENANT'];
const PAGE_SIZE = 10;

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

export default function Units() {
  const queryClient = useQueryClient();

  // ---- filters / pagination ----
  const [page, setPage] = useState(0);
  const [typeFilter, setTypeFilter] = useState('');
  const [occupancyFilter, setOccupancyFilter] = useState('');

  const query = useQuery({
    queryKey: ['units', { page, type: typeFilter, occupancy: occupancyFilter }],
    queryFn: () =>
      getUnitsApi({
        page,
        size: PAGE_SIZE,
        type: typeFilter || undefined,
        occupancy: occupancyFilter || undefined,
      }),
    keepPreviousData: true,
  });

  const rows = query.data?.content || [];
  const totalPages = query.data?.totalPages || 0;

  // ---- mutations ----
  const createMutation = useMutation({
    mutationFn: createUnitApi,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['units'] }),
  });
  const updateMutation = useMutation({
    mutationFn: ({ id, body }) => updateUnitApi(id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['units'] }),
  });
  const deleteMutation = useMutation({
    mutationFn: deleteUnitApi,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['units'] }),
  });

  // ---- dialog state ----
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [membersOf, setMembersOf] = useState(null);  // unit id whose members are shown

  const handleDelete = (u) => {
    if (window.confirm(`Soft-delete unit "${u.unitNumber}"?`)) {
      deleteMutation.mutate(u.id);
    }
  };

  const columns = [
    { field: 'unitNumber', headerName: 'Unit #' },
    {
      field: 'unitType',
      headerName: 'Type',
      renderCell: (r) => (
        <Chip
          size="small"
          label={r.unitType}
          color={r.unitType === 'FLAT' ? 'primary'
            : r.unitType === 'SHOP' ? 'warning' : 'default'}
        />
      ),
    },
    { field: 'floor', headerName: 'Floor', renderCell: (r) => r.floor || '—' },
    { field: 'ownerName', headerName: 'Owner', renderCell: (r) => r.ownerName || '—' },
    { field: 'occupancy', headerName: 'Occupancy' },
    {
      field: 'baseMaintenance',
      headerName: 'Base',
      renderCell: (r) => currency(r.baseMaintenance),
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
          <Tooltip title="Members">
            <IconButton size="small" onClick={() => setMembersOf(r.id)}>
              <PeopleIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Edit">
            <IconButton size="small" onClick={() => setEditing(r)}>
              <EditIcon fontSize="small" />
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
      <Typography variant="h5" sx={{ mb: 2 }}>Units</Typography>

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
          label="Type"
          select
          size="small"
          value={typeFilter}
          onChange={(e) => { setTypeFilter(e.target.value); setPage(0); }}
          sx={{ minWidth: 140 }}
        >
          <MenuItem value="">All</MenuItem>
          {TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
        </TextField>
        <TextField
          label="Occupancy"
          select
          size="small"
          value={occupancyFilter}
          onChange={(e) => { setOccupancyFilter(e.target.value); setPage(0); }}
          sx={{ minWidth: 140 }}
        >
          <MenuItem value="">All</MenuItem>
          {OCCUPANCIES.map((o) => <MenuItem key={o} value={o}>{o}</MenuItem>)}
        </TextField>
        <Box sx={{ flexGrow: 1 }} />
        <Button startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
          Add Unit
        </Button>
      </Toolbar>

      {query.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {query.error?.response?.data?.message || 'Failed to load units'}
        </Alert>
      )}

      <DataTable
        columns={columns}
        rows={rows}
        loading={query.isFetching}
        emptyText="No units yet — click Add Unit."
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

      <UnitFormDialog
        open={createOpen}
        title="Add Unit"
        onClose={() => setCreateOpen(false)}
        onSubmit={(body) =>
          createMutation.mutateAsync(body).then(() => setCreateOpen(false))
        }
        submitting={createMutation.isPending}
        error={createMutation.error?.response?.data?.message}
      />

      <UnitFormDialog
        open={!!editing}
        title="Edit Unit"
        initial={editing}
        onClose={() => setEditing(null)}
        onSubmit={(body) =>
          updateMutation
            .mutateAsync({ id: editing.id, body })
            .then(() => setEditing(null))
        }
        submitting={updateMutation.isPending}
        error={updateMutation.error?.response?.data?.message}
      />

      <UnitMembersDialog
        unitId={membersOf}
        onClose={() => setMembersOf(null)}
      />
    </Box>
  );
}

function UnitFormDialog({
  open,
  title,
  initial,
  onClose,
  onSubmit,
  submitting,
  error,
}) {
  const empty = {
    unitType: 'FLAT',
    unitNumber: '',
    floor: '',
    ownerMemberId: '',
    occupancy: 'OWNER',
    baseMaintenance: '0',
    tenantSurcharge: '0',
  };
  const [form, setForm] = useState(empty);

  useEffect(() => {
    if (!open) return;
    setForm(initial
      ? {
        unitType: initial.unitType || 'FLAT',
        unitNumber: initial.unitNumber || '',
        floor: initial.floor || '',
        ownerMemberId: initial.ownerMemberId ?? '',
        occupancy: initial.occupancy || 'OWNER',
        baseMaintenance: String(initial.baseMaintenance ?? '0'),
        tenantSurcharge: String(initial.tenantSurcharge ?? '0'),
      }
      : empty);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, initial]);

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const handleSubmit = () => {
    const body = {
      unitType: form.unitType,
      unitNumber: form.unitNumber,
      floor: form.floor || null,
      ownerMemberId: form.ownerMemberId ? Number(form.ownerMemberId) : null,
      occupancy: form.occupancy,
      baseMaintenance: form.baseMaintenance || '0',
      tenantSurcharge: form.tenantSurcharge || '0',
    };
    onSubmit(body);
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
            <TextField label="Unit type" select required value={form.unitType} onChange={set('unitType')}>
              {TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
            </TextField>
            <TextField label="Unit number" required value={form.unitNumber} onChange={set('unitNumber')} />
            <TextField label="Floor" value={form.floor} onChange={set('floor')} />
            <TextField
              label="Owner member ID"
              type="number"
              value={form.ownerMemberId}
              onChange={set('ownerMemberId')}
              helperText="Numeric member.id; leave blank if none"
            />
            <TextField label="Occupancy" select required value={form.occupancy} onChange={set('occupancy')}>
              {OCCUPANCIES.map((o) => <MenuItem key={o} value={o}>{o}</MenuItem>)}
            </TextField>
            <TextField
              label="Base maintenance (₹)"
              type="number"
              value={form.baseMaintenance}
              onChange={set('baseMaintenance')}
              inputProps={{ step: '0.01', min: '0' }}
            />
            <TextField
              label="Tenant surcharge (₹)"
              type="number"
              value={form.tenantSurcharge}
              onChange={set('tenantSurcharge')}
              inputProps={{ step: '0.01', min: '0' }}
            />
          </Box>
          {error && <Alert severity="error">{error}</Alert>}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button variant="text" onClick={onClose} disabled={submitting}>Cancel</Button>
        <Button
          onClick={handleSubmit}
          disabled={submitting || !form.unitType || !form.unitNumber || !form.occupancy}
        >
          Save
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function UnitMembersDialog({ unitId, onClose }) {
  const queryClient = useQueryClient();
  const enabled = !!unitId;

  const detailQuery = useQuery({
    queryKey: ['unit', unitId],
    queryFn: () => getUnitApi(unitId),
    enabled,
  });

  const [memberId, setMemberId] = useState('');
  const [relationship, setRelationship] = useState('OWNER');

  useEffect(() => {
    if (enabled) {
      setMemberId('');
      setRelationship('OWNER');
    }
  }, [enabled]);

  const linkMutation = useMutation({
    mutationFn: (body) => linkMemberApi(unitId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['unit', unitId] });
      queryClient.invalidateQueries({ queryKey: ['units'] });
      setMemberId('');
    },
  });

  const unlinkMutation = useMutation({
    mutationFn: (mId) => unlinkMemberApi(unitId, mId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['unit', unitId] });
      queryClient.invalidateQueries({ queryKey: ['units'] });
    },
  });

  const members = detailQuery.data?.members || [];

  return (
    <Dialog open={enabled} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>
        Unit Members{detailQuery.data ? ` — ${detailQuery.data.unitNumber}` : ''}
      </DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          {detailQuery.isError && (
            <Alert severity="error">
              {detailQuery.error?.response?.data?.message || 'Failed to load unit'}
            </Alert>
          )}

          <Typography variant="subtitle2">Linked members</Typography>
          {members.length === 0 && !detailQuery.isFetching ? (
            <Typography variant="body2" color="text.secondary">
              No members linked yet.
            </Typography>
          ) : (
            <List dense sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1 }}>
              {members.map((m) => (
                <ListItem
                  key={m.memberId}
                  secondaryAction={
                    <IconButton
                      edge="end"
                      size="small"
                      color="error"
                      onClick={() => {
                        if (window.confirm(`Unlink ${m.memberName}?`)) {
                          unlinkMutation.mutate(m.memberId);
                        }
                      }}
                    >
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  }
                >
                  <ListItemText
                    primary={`${m.memberName} (#${m.memberId})`}
                    secondary={`${m.memberPhone} — ${m.relationship}`}
                  />
                </ListItem>
              ))}
            </List>
          )}

          <Divider />

          <Typography variant="subtitle2">Link a member</Typography>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
            <TextField
              label="Member ID"
              type="number"
              value={memberId}
              onChange={(e) => setMemberId(e.target.value)}
            />
            <TextField
              label="Relationship"
              select
              value={relationship}
              onChange={(e) => setRelationship(e.target.value)}
            >
              {RELATIONSHIPS.map((r) => (
                <MenuItem key={r} value={r}>{r}</MenuItem>
              ))}
            </TextField>
          </Box>

          {(linkMutation.error || unlinkMutation.error) && (
            <Alert severity="error">
              {(linkMutation.error?.response?.data?.message
                || unlinkMutation.error?.response?.data?.message)}
            </Alert>
          )}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button variant="text" onClick={onClose}>Close</Button>
        <Button
          onClick={() =>
            linkMutation.mutate({
              memberId: Number(memberId),
              relationship,
            })
          }
          disabled={!memberId || linkMutation.isPending}
        >
          Link
        </Button>
      </DialogActions>
    </Dialog>
  );
}
