import { useState } from 'react';
import { useQueries, useQuery } from '@tanstack/react-query';
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Alert,
  Box,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';

import { getUnitsApi } from '../../api/units.js';
import {
  getUnitChargesApi,
  getUnitOutstandingApi,
} from '../../api/charges.js';

const MONTHS = [
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
];

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

const statusColor = (s) =>
  s === 'PAID' ? 'success'
    : s === 'PARTIAL' ? 'warning'
    : s === 'VOID' ? 'default'
    : 'error';

export default function MyDues() {
  // GET /units is member-aware: admin sees everything; a member sees only
  // units they own / are linked to (see UnitService.listUnits caller-scope).
  const unitsQuery = useQuery({
    queryKey: ['member-units'],
    queryFn: () => getUnitsApi({ page: 0, size: 50 }),
    retry: false,
  });

  const units = unitsQuery.data?.content || [];

  // Fan out one outstanding query per unit.
  const outstandingQueries = useQueries({
    queries: units.map((u) => ({
      queryKey: ['unit-outstanding', u.id],
      queryFn: () => getUnitOutstandingApi(u.id),
      enabled: !!u.id,
    })),
  });

  if (unitsQuery.isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ p: { xs: 2, sm: 3 }, maxWidth: 960, mx: 'auto' }}>
      <Typography variant="h5" sx={{ mb: 2 }}>My Dues</Typography>

      {unitsQuery.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {unitsQuery.error?.response?.data?.message
            || 'Failed to load your units'}
        </Alert>
      )}

      {!unitsQuery.isError && units.length === 0 && (
        <Alert severity="info">No units linked to your account.</Alert>
      )}

      <Stack spacing={2}>
        {units.map((u, idx) => {
          const outQuery = outstandingQueries[idx];
          const outstanding = outQuery?.data?.outstanding;
          return (
            <Card key={u.id} variant="outlined">
              <CardContent>
                <Stack
                  direction={{ xs: 'column', sm: 'row' }}
                  spacing={1}
                  justifyContent="space-between"
                  alignItems={{ sm: 'center' }}
                  sx={{ mb: 1.5 }}
                >
                  <Box>
                    <Typography variant="h6">
                      {u.unitNumber}{' '}
                      <Typography component="span" variant="caption" color="text.secondary">
                        ({u.unitType}, {u.occupancy})
                      </Typography>
                    </Typography>
                  </Box>
                  <Box sx={{ textAlign: { sm: 'right' } }}>
                    <Typography variant="caption" color="text.secondary">
                      Outstanding
                    </Typography>
                    <Typography
                      variant="h6"
                      color={Number(outstanding) > 0 ? 'error' : 'success.main'}
                    >
                      {outQuery?.isLoading ? '…' : currency(outstanding ?? '0')}
                    </Typography>
                  </Box>
                </Stack>

                <UnitChargeHistory unitId={u.id} />
              </CardContent>
            </Card>
          );
        })}
      </Stack>
    </Box>
  );
}

function UnitChargeHistory({ unitId }) {
  const [open, setOpen] = useState(false);
  return (
    <Accordion
      expanded={open}
      onChange={() => setOpen((v) => !v)}
      disableGutters
      square
      sx={{ boxShadow: 'none', '&:before': { display: 'none' } }}
    >
      <AccordionSummary expandIcon={<ExpandMoreIcon />} sx={{ px: 0 }}>
        <Typography variant="subtitle2">View charge history</Typography>
      </AccordionSummary>
      <AccordionDetails sx={{ px: 0 }}>
        {open && <ChargeRows unitId={unitId} />}
      </AccordionDetails>
    </Accordion>
  );
}

function ChargeRows({ unitId }) {
  const query = useQuery({
    queryKey: ['unit-charges', unitId],
    queryFn: () => getUnitChargesApi(unitId),
    enabled: !!unitId,
  });

  if (query.isLoading) {
    return <CircularProgress size={20} />;
  }
  if (query.isError) {
    return (
      <Alert severity="error">
        {query.error?.response?.data?.message || 'Failed to load charges'}
      </Alert>
    );
  }
  const rows = query.data || [];
  if (rows.length === 0) {
    return <Typography variant="body2" color="text.secondary">No charges yet.</Typography>;
  }

  return (
    <Box sx={{ overflowX: 'auto' }}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Period</TableCell>
            <TableCell align="right">Due</TableCell>
            <TableCell align="right">Paid</TableCell>
            <TableCell align="right">Outstanding</TableCell>
            <TableCell>Status</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((c) => (
            <TableRow key={c.id}>
              <TableCell>{`${MONTHS[c.periodMonth - 1]} ${c.periodYear}`}</TableCell>
              <TableCell align="right">{currency(c.amountDue)}</TableCell>
              <TableCell align="right">{currency(c.amountPaid)}</TableCell>
              <TableCell align="right">{currency(c.outstanding)}</TableCell>
              <TableCell>
                <Chip size="small" label={c.status} color={statusColor(c.status)} />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Box>
  );
}
