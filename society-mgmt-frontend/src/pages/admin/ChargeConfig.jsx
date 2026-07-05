import { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import SaveIcon from '@mui/icons-material/Save';
import CloseIcon from '@mui/icons-material/Close';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { getChargeConfigApi, updateChargeConfigApi } from '../../api/charges.js';

const emptyForm = {
  societyName: '',
  defaultFlatMaintenance: '0',
  defaultShopMaintenance: '0',
  defaultTenantSurcharge: '0',
  defaultDueDay: '10',
  societyUpiId: '',
  societyBankDetails: '',
};

function toForm(c) {
  if (!c) return emptyForm;
  return {
    societyName: c.societyName ?? '',
    defaultFlatMaintenance: String(c.defaultFlatMaintenance ?? '0'),
    defaultShopMaintenance: String(c.defaultShopMaintenance ?? '0'),
    defaultTenantSurcharge: String(c.defaultTenantSurcharge ?? '0'),
    defaultDueDay: String(c.defaultDueDay ?? '10'),
    societyUpiId: c.societyUpiId ?? '',
    societyBankDetails: c.societyBankDetails ?? '',
  };
}

export default function ChargeConfig() {
  const queryClient = useQueryClient();
  const query = useQuery({ queryKey: ['charge-config'], queryFn: getChargeConfigApi });

  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState(emptyForm);

  useEffect(() => {
    if (query.data && !editing) setForm(toForm(query.data));
  }, [query.data, editing]);

  const updateMutation = useMutation({
    mutationFn: updateChargeConfigApi,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['charge-config'] });
      setEditing(false);
    },
  });

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const handleSave = () => {
    const body = {
      societyName: form.societyName,
      defaultFlatMaintenance: form.defaultFlatMaintenance || '0',
      defaultShopMaintenance: form.defaultShopMaintenance || '0',
      defaultTenantSurcharge: form.defaultTenantSurcharge || '0',
      defaultDueDay: Number(form.defaultDueDay) || 10,
      societyUpiId: form.societyUpiId || null,
      societyBankDetails: form.societyBankDetails || null,
    };
    updateMutation.mutate(body);
  };

  if (query.isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ p: { xs: 2, sm: 3 }, maxWidth: 760, mx: 'auto' }}>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={1}
        alignItems={{ sm: 'center' }}
        justifyContent="space-between"
        sx={{ mb: 2 }}
      >
        <Typography variant="h5">Charge Configuration</Typography>
        {!editing ? (
          <Button
            startIcon={<EditIcon />}
            variant="outlined"
            onClick={() => setEditing(true)}
          >
            Edit
          </Button>
        ) : (
          <Stack direction="row" spacing={1}>
            <Button
              startIcon={<CloseIcon />}
              variant="text"
              onClick={() => {
                setForm(toForm(query.data));
                setEditing(false);
                updateMutation.reset();
              }}
              disabled={updateMutation.isPending}
            >
              Cancel
            </Button>
            <Button
              startIcon={<SaveIcon />}
              variant="contained"
              onClick={handleSave}
              disabled={updateMutation.isPending || !form.societyName}
            >
              Save
            </Button>
          </Stack>
        )}
      </Stack>

      {query.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {query.error?.response?.data?.message || 'Failed to load configuration'}
        </Alert>
      )}
      {updateMutation.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {updateMutation.error?.response?.data?.message
            || 'Failed to update configuration'}
        </Alert>
      )}

      <Paper variant="outlined" sx={{ p: { xs: 2, sm: 3 } }}>
        <Box
          sx={{
            display: 'grid',
            gap: 2,
            gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' },
          }}
        >
          <TextField
            label="Society name"
            value={form.societyName}
            onChange={set('societyName')}
            disabled={!editing}
            required
            sx={{ gridColumn: { xs: '1', sm: '1 / span 2' } }}
          />
          <TextField
            label="Default flat maintenance (₹)"
            type="number"
            value={form.defaultFlatMaintenance}
            onChange={set('defaultFlatMaintenance')}
            disabled={!editing}
            inputProps={{ step: '0.01', min: '0' }}
          />
          <TextField
            label="Default shop maintenance (₹)"
            type="number"
            value={form.defaultShopMaintenance}
            onChange={set('defaultShopMaintenance')}
            disabled={!editing}
            inputProps={{ step: '0.01', min: '0' }}
          />
          <TextField
            label="Default tenant surcharge (₹)"
            type="number"
            value={form.defaultTenantSurcharge}
            onChange={set('defaultTenantSurcharge')}
            disabled={!editing}
            inputProps={{ step: '0.01', min: '0' }}
          />
          <TextField
            label="Default due day (1–28)"
            type="number"
            value={form.defaultDueDay}
            onChange={set('defaultDueDay')}
            disabled={!editing}
            inputProps={{ min: 1, max: 28 }}
          />
          <TextField
            label="Society UPI ID"
            value={form.societyUpiId}
            onChange={set('societyUpiId')}
            disabled={!editing}
            helperText="Shown on member payment screen"
          />
          <TextField
            label="Society bank details"
            value={form.societyBankDetails}
            onChange={set('societyBankDetails')}
            disabled={!editing}
            multiline
            minRows={2}
            sx={{ gridColumn: { xs: '1', sm: '1 / span 2' } }}
          />
        </Box>

        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 2 }}>
          These defaults seed new units and set the due date used by monthly
          charge generation. Changes do not retro-edit existing charges.
        </Typography>
      </Paper>
    </Box>
  );
}
