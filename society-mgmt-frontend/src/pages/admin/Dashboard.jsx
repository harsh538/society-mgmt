import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Divider,
  MenuItem,
  Skeleton,
  Stack,
  TextField,
  Toolbar,
  Typography,
} from '@mui/material';
import HourglassTopIcon from '@mui/icons-material/HourglassTop';
import { useQuery } from '@tanstack/react-query';

import DataTable from '../../components/DataTable.jsx';
import {
  getDashboardSummaryApi,
  getFinancialsApi,
  getMaintenanceStatusApi,
} from '../../api/dashboard.js';

const TYPE_OPTIONS = ['FLAT', 'SHOP', 'TERRACE'];
const STATUS_OPTIONS = ['DUE', 'PARTIAL', 'PAID', 'NO_CHARGES'];

const STATUS_COLORS = {
  DUE: 'warning',
  PARTIAL: 'info',
  PAID: 'success',
  NO_CHARGES: 'default',
  VOID: 'default',
};

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

function StatCard({ label, value, sub, valueColor, action }) {
  return (
    <Card variant="outlined" sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="body2" color="text.secondary">
          {label}
        </Typography>
        <Typography
          variant="h4"
          sx={{ mt: 0.5, fontWeight: 600, color: valueColor || 'text.primary' }}
        >
          {value}
        </Typography>
        {sub && (
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
            {sub}
          </Typography>
        )}
        {action && <Box sx={{ mt: 1 }}>{action}</Box>}
      </CardContent>
    </Card>
  );
}

function SectionHeader({ title, subtitle, right }) {
  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      justifyContent="space-between"
      alignItems={{ sm: 'center' }}
      spacing={1}
      sx={{ mb: 2 }}
    >
      <Box>
        <Typography variant="h6">{title}</Typography>
        {subtitle && (
          <Typography variant="body2" color="text.secondary">
            {subtitle}
          </Typography>
        )}
      </Box>
      {right}
    </Stack>
  );
}

export default function AdminDashboard() {
  // ---- Section 1: unit summary ---------------------------------------------
  const summaryQuery = useQuery({
    queryKey: ['dashboard-summary'],
    queryFn: getDashboardSummaryApi,
  });
  const summary = summaryQuery.data;

  // ---- Section 2: financials -----------------------------------------------
  const finQuery = useQuery({
    queryKey: ['dashboard-financials'],
    queryFn: getFinancialsApi,
    refetchInterval: 60000,
  });
  const fin = finQuery.data;

  // ---- Section 3: maintenance status ---------------------------------------
  const [typeFilter, setTypeFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const maintQuery = useQuery({
    queryKey: ['dashboard-maintenance', typeFilter, statusFilter],
    queryFn: () =>
      getMaintenanceStatusApi({
        type: typeFilter || undefined,
        status: statusFilter || undefined,
      }),
    keepPreviousData: true,
  });

  const maintRows = maintQuery.data || [];

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
    { field: 'occupancy', headerName: 'Occupancy' },
    { field: 'ownerName', headerName: 'Owner', renderCell: (r) => r.ownerName || '—' },
    {
      field: 'amountDue',
      headerName: 'Amount Due',
      renderCell: (r) => currency(r.amountDue),
    },
    {
      field: 'amountPaid',
      headerName: 'Paid',
      renderCell: (r) => currency(r.amountPaid),
    },
    {
      field: 'outstanding',
      headerName: 'Outstanding',
      renderCell: (r) => (
        <Typography
          component="span"
          sx={{
            fontWeight: 600,
            color: Number(r.outstanding) > 0 ? 'error.main' : 'text.primary',
          }}
        >
          {currency(r.outstanding)}
        </Typography>
      ),
    },
    {
      field: 'overallStatus',
      headerName: 'Status',
      renderCell: (r) => (
        <Chip
          size="small"
          label={r.overallStatus}
          color={STATUS_COLORS[r.overallStatus] || 'default'}
        />
      ),
    },
  ];

  return (
    <Box sx={{ maxWidth: 1400, mx: 'auto' }}>
      {/* ---- Section 1: Unit Summary ---- */}
      <SectionHeader title="Unit Summary" subtitle="Active units in the society" />
      {summaryQuery.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {summaryQuery.error?.response?.data?.message || 'Failed to load unit summary'}
        </Alert>
      )}
      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: 'repeat(4, 1fr)' },
          mb: 3,
        }}
      >
        {summaryQuery.isLoading ? (
          [...Array(4)].map((_, i) => (
            <Skeleton key={i} variant="rectangular" height={110} />
          ))
        ) : (
          <>
            <StatCard
              label="Total Units"
              value={summary?.totalUnits ?? 0}
              sub={`Flats: ${summary?.flatCount ?? 0} | Shops: ${summary?.shopCount ?? 0} | Terrace: ${summary?.terraceCount ?? 0}`}
            />
            <StatCard label="Owner Occupied" value={summary?.ownerOccupied ?? 0} />
            <StatCard label="Tenant Occupied" value={summary?.tenantOccupied ?? 0} />
            <StatCard label="Vacant" value={summary?.vacant ?? 0} />
          </>
        )}
      </Box>

      <Divider sx={{ my: 3 }} />

      {/* ---- Section 2: Financials ---- */}
      <SectionHeader
        title="Financial Summary"
        subtitle="Over non-VOID maintenance charges (auto-refreshes every 60s)"
      />
      {finQuery.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {finQuery.error?.response?.data?.message || 'Failed to load financials'}
        </Alert>
      )}
      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: {
            xs: '1fr',
            sm: '1fr 1fr',
            md: 'repeat(3, 1fr)',
            lg: 'repeat(6, 1fr)',
          },
          mb: 3,
        }}
      >
        {finQuery.isLoading ? (
          [...Array(6)].map((_, i) => (
            <Skeleton key={i} variant="rectangular" height={130} />
          ))
        ) : (
          <>
            <StatCard label="Total Billed" value={currency(fin?.totalBilled)} />
            <StatCard label="Total Collected" value={currency(fin?.totalCollected)} />
            <StatCard
              label="Total Outstanding"
              value={currency(fin?.totalOutstanding)}
              valueColor={Number(fin?.totalOutstanding) > 0 ? 'error.main' : 'text.primary'}
            />
            <StatCard
              label="Total Expenses"
              value={currency(fin?.totalExpenses)}
              sub="Society running costs"
            />
            <StatCard
              label="Net Position"
              value={currency(fin?.netPosition)}
              valueColor={Number(fin?.netPosition) >= 0 ? 'success.main' : 'error.main'}
              sub="Collected − Expenses"
            />
            <StatCard
              label="Pending Payments"
              value={
                <Chip
                  icon={<HourglassTopIcon />}
                  label={fin?.pendingPaymentsCount ?? 0}
                  color="warning"
                  sx={{ fontSize: '1.25rem', height: 36, px: 1 }}
                />
              }
              action={
                <Button
                  size="small"
                  component={RouterLink}
                  to="/admin/payment-queue"
                  variant="text"
                >
                  Open queue →
                </Button>
              }
            />
          </>
        )}
      </Box>

      <Divider sx={{ my: 3 }} />

      {/* ---- Section 3: Maintenance Status ---- */}
      <SectionHeader
        title="Maintenance Status"
        subtitle="Per-unit due / paid / outstanding (over non-VOID charges)"
        right={
          <Chip
            label={`${maintRows.length} unit${maintRows.length === 1 ? '' : 's'}`}
            size="small"
          />
        }
      />

      <Toolbar disableGutters sx={{ gap: 2, flexWrap: 'wrap', mb: 1, px: 0 }}>
        <TextField
          select
          size="small"
          label="Type"
          value={typeFilter}
          onChange={(e) => setTypeFilter(e.target.value)}
          sx={{ minWidth: 140 }}
        >
          <MenuItem value="">All types</MenuItem>
          {TYPE_OPTIONS.map((t) => (
            <MenuItem key={t} value={t}>
              {t}
            </MenuItem>
          ))}
        </TextField>
        <TextField
          select
          size="small"
          label="Status"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          sx={{ minWidth: 160 }}
        >
          <MenuItem value="">All statuses</MenuItem>
          {STATUS_OPTIONS.map((s) => (
            <MenuItem key={s} value={s}>
              {s}
            </MenuItem>
          ))}
        </TextField>
        {(typeFilter || statusFilter) && (
          <Button
            size="small"
            onClick={() => {
              setTypeFilter('');
              setStatusFilter('');
            }}
          >
            Clear
          </Button>
        )}
      </Toolbar>

      {maintQuery.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {maintQuery.error?.response?.data?.message || 'Failed to load maintenance status'}
        </Alert>
      )}

      <DataTable
        columns={columns}
        rows={maintRows}
        loading={maintQuery.isFetching}
        emptyText="No units match the selected filters."
      />
    </Box>
  );
}
