import {
  Alert,
  Box,
  Card,
  CardContent,
  Chip,
  Skeleton,
  Stack,
  Typography,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';

import DataTable from '../../components/DataTable.jsx';
import { getOutstandingApi } from '../../api/dashboard.js';

const TYPE_COLORS = {
  FLAT: 'primary',
  SHOP: 'secondary',
  TERRACE: 'default',
};

const currency = (v) => {
  if (v === null || v === undefined || v === '') return '₹0.00';
  const n = Number(v);
  if (Number.isNaN(n)) return String(v);
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
    minimumFractionDigits: 2,
  }).format(n);
};

/**
 * Society-wide outstanding view (project.md § 5.9 / § 3.10).
 *
 * Society total at top, then per-unit list of units that still owe money.
 */
export default function Outstanding() {
  const query = useQuery({
    queryKey: ['outstanding'],
    queryFn: getOutstandingApi,
    refetchInterval: 60000,
  });

  const data = query.data;
  const rows = data?.units || [];
  const societyTotal = data?.societyTotal;

  const columns = [
    { field: 'unitNumber', headerName: 'Unit #' },
    {
      field: 'unitType',
      headerName: 'Type',
      renderCell: (r) => (
        <Chip
          size="small"
          label={r.unitType}
          color={TYPE_COLORS[r.unitType] || 'default'}
          variant="outlined"
        />
      ),
    },
    { field: 'ownerName', headerName: 'Owner', renderCell: (r) => r.ownerName || '—' },
    {
      field: 'outstanding',
      headerName: 'Outstanding',
      renderCell: (r) => (
        <Typography component="span" sx={{ fontWeight: 600, color: 'error.main' }}>
          {currency(r.outstanding)}
        </Typography>
      ),
    },
  ];

  return (
    <Box sx={{ p: { xs: 2, sm: 3 }, maxWidth: 1100, mx: 'auto' }}>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        justifyContent="space-between"
        alignItems={{ sm: 'center' }}
        spacing={1}
        sx={{ mb: 3 }}
      >
        <Box>
          <Typography variant="h5">Outstanding Balances</Typography>
          <Typography variant="body2" color="text.secondary">
            Society-wide and per-unit. Derived from non-VOID maintenance charges.
          </Typography>
        </Box>
      </Stack>

      {/* Society total stat card */}
      {query.isLoading ? (
        <Skeleton variant="rectangular" height={140} sx={{ mb: 3 }} />
      ) : (
        <Card variant="outlined" sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="body2" color="text.secondary">
              Society Total Outstanding
            </Typography>
            <Typography
              variant="h3"
              sx={{
                fontWeight: 700,
                mt: 1,
                color: Number(societyTotal) > 0 ? 'error.main' : 'success.main',
              }}
            >
              {currency(societyTotal)}
            </Typography>
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
              {rows.length} unit{rows.length === 1 ? '' : 's'} with outstanding dues
            </Typography>
          </CardContent>
        </Card>
      )}

      {query.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {query.error?.response?.data?.message || 'Failed to load outstanding'}
        </Alert>
      )}

      <DataTable
        columns={columns}
        rows={rows}
        loading={query.isFetching}
        emptyText="No outstanding dues. All units are paid up."
      />
    </Box>
  );
}
