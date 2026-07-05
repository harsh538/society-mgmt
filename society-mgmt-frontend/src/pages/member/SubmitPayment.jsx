import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import { useMutation, useQuery } from '@tanstack/react-query';

import { getChargeConfigApi, getUnitChargesApi } from '../../api/charges.js';
import { getUnitsApi } from '../../api/units.js';
import { submitPaymentApi } from '../../api/payments.js';

const PAYMENT_TYPES = ['MAINTENANCE', 'OTHER'];
const METHODS = ['UPI', 'BANK_TRANSFER', 'CASH'];
const ACCEPT_MIME = 'image/jpeg,image/png,application/pdf';
const MAX_BYTES = 5 * 1024 * 1024;

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

// HTML datetime-local input wants `yyyy-MM-ddTHH:mm` in local time, no timezone.
const nowLocalInput = () => {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

export default function SubmitPayment() {
  const navigate = useNavigate();

  const configQuery = useQuery({
    queryKey: ['charge-config'],
    queryFn: getChargeConfigApi,
    retry: false,
  });

  const unitsQuery = useQuery({
    queryKey: ['member-units'],
    queryFn: () => getUnitsApi({ page: 0, size: 50 }),
  });
  const units = unitsQuery.data?.content || [];

  const [unitId, setUnitId] = useState('');
  const [chargeId, setChargeId] = useState('');
  const [paymentType, setPaymentType] = useState('MAINTENANCE');
  const [amount, setAmount] = useState('');
  const [method, setMethod] = useState('UPI');
  const [utrReference, setUtrReference] = useState('');
  const [paidAt, setPaidAt] = useState(nowLocalInput());
  const [proofFile, setProofFile] = useState(null);
  const [fileError, setFileError] = useState('');
  const [success, setSuccess] = useState(false);

  const chargesQuery = useQuery({
    queryKey: ['unit-charges', unitId],
    queryFn: () => getUnitChargesApi(unitId),
    enabled: !!unitId,
  });
  const dueCharges = useMemo(
    () =>
      (chargesQuery.data || []).filter(
        (c) => c.status === 'DUE' || c.status === 'PARTIAL',
      ),
    [chargesQuery.data],
  );

  useEffect(() => {
    if (!chargeId) return;
    const c = dueCharges.find((x) => String(x.id) === String(chargeId));
    if (c) {
      const outstanding = Number(c.outstanding ?? c.amountDue) || 0;
      setAmount(outstanding > 0 ? String(outstanding) : '');
    }
  }, [chargeId, dueCharges]);

  const submitMutation = useMutation({
    mutationFn: submitPaymentApi,
    onSuccess: () => {
      setSuccess(true);
      setTimeout(() => navigate('/member/payments'), 1500);
    },
  });

  const onFileChange = (e) => {
    const f = e.target.files?.[0] || null;
    setFileError('');
    if (!f) {
      setProofFile(null);
      return;
    }
    if (!ACCEPT_MIME.split(',').includes(f.type)) {
      setFileError('Only JPEG, PNG or PDF accepted.');
      return;
    }
    if (f.size > MAX_BYTES) {
      setFileError('File exceeds 5 MB limit.');
      return;
    }
    setProofFile(f);
  };

  const canSubmit =
    !!unitId &&
    !!amount &&
    Number(amount) > 0 &&
    !!method &&
    !!paymentType &&
    !!paidAt &&
    !fileError &&
    !submitMutation.isPending;

  const handleSubmit = () => {
    setSuccess(false);
    const payload = {
      unitId: Number(unitId),
      chargeId: chargeId ? Number(chargeId) : null,
      paymentType,
      amount,
      method,
      utrReference: utrReference || null,
      // ISO string with timezone — convert local input to OffsetDateTime.
      paidAt: new Date(paidAt).toISOString(),
    };
    const formData = new FormData();
    formData.append(
      'data',
      new Blob([JSON.stringify(payload)], { type: 'application/json' }),
    );
    if (proofFile) {
      formData.append('proof', proofFile);
    }
    submitMutation.mutate(formData);
  };

  return (
    <Box sx={{ p: { xs: 2, sm: 3 }, maxWidth: 720, mx: 'auto' }}>
      <Typography variant="h5" sx={{ mb: 2 }}>
        Submit Payment
      </Typography>

      {configQuery.data && (
        <Card variant="outlined" sx={{ mb: 2, bgcolor: 'action.hover' }}>
          <CardContent>
            <Typography variant="subtitle2" gutterBottom>
              Pay to {configQuery.data.societyName}
            </Typography>
            <Typography variant="body2">
              UPI: <strong>{configQuery.data.societyUpiId || '—'}</strong>
            </Typography>
            {configQuery.data.societyBankDetails && (
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', mt: 0.5 }}>
                {configQuery.data.societyBankDetails}
              </Typography>
            )}
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
              After paying, fill in the form below with the UTR / reference number
              and upload a screenshot. The admin will verify and issue a receipt.
            </Typography>
          </CardContent>
        </Card>
      )}

      <Paper elevation={1} sx={{ p: { xs: 2, sm: 3 } }}>
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' },
            gap: 2,
          }}
        >
          <TextField
            label="Unit"
            select
            required
            value={unitId}
            onChange={(e) => {
              setUnitId(e.target.value);
              setChargeId('');
              setAmount('');
            }}
            disabled={unitsQuery.isLoading}
          >
            {units.length === 0 && (
              <MenuItem value="" disabled>
                {unitsQuery.isLoading ? 'Loading…' : 'No units available'}
              </MenuItem>
            )}
            {units.map((u) => (
              <MenuItem key={u.id} value={u.id}>
                {u.unitNumber} ({u.unitType})
              </MenuItem>
            ))}
          </TextField>

          <TextField
            label="Payment Type"
            select
            required
            value={paymentType}
            onChange={(e) => setPaymentType(e.target.value)}
          >
            {PAYMENT_TYPES.map((t) => (
              <MenuItem key={t} value={t}>
                {t}
              </MenuItem>
            ))}
          </TextField>

          <TextField
            label="Charge (optional)"
            select
            value={chargeId}
            onChange={(e) => setChargeId(e.target.value)}
            disabled={!unitId || chargesQuery.isFetching}
            helperText={
              !unitId
                ? 'Pick a unit first'
                : chargesQuery.isFetching
                ? 'Loading charges…'
                : 'Optional — link payment to a specific monthly charge'
            }
          >
            <MenuItem value="">— None / advance —</MenuItem>
            {dueCharges.map((c) => (
              <MenuItem key={c.id} value={c.id}>
                {`${c.periodYear}-${String(c.periodMonth).padStart(2, '0')}`} —{' '}
                outstanding {currency(c.outstanding)} ({c.status})
              </MenuItem>
            ))}
          </TextField>

          <TextField
            label="Amount (₹)"
            type="number"
            required
            inputProps={{ step: '0.01', min: '0.01' }}
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
          />

          <TextField
            label="Method"
            select
            required
            value={method}
            onChange={(e) => setMethod(e.target.value)}
          >
            {METHODS.map((m) => (
              <MenuItem key={m} value={m}>
                {m}
              </MenuItem>
            ))}
          </TextField>

          <TextField
            label="UTR / Reference"
            value={utrReference}
            onChange={(e) => setUtrReference(e.target.value)}
            placeholder="e.g. 412345678901"
          />

          <TextField
            label="Paid At"
            type="datetime-local"
            required
            value={paidAt}
            onChange={(e) => setPaidAt(e.target.value)}
            InputLabelProps={{ shrink: true }}
            sx={{ gridColumn: { md: 'span 2' } }}
          />

          <Box sx={{ gridColumn: { md: 'span 2' } }}>
            <Button
              component="label"
              variant="outlined"
              startIcon={<UploadFileIcon />}
              fullWidth
              sx={{ justifyContent: 'flex-start', textTransform: 'none' }}
            >
              {proofFile ? proofFile.name : 'Upload proof (JPEG / PNG / PDF, max 5 MB)'}
              <input
                type="file"
                hidden
                accept={ACCEPT_MIME}
                onChange={onFileChange}
              />
            </Button>
            {fileError && (
              <Alert severity="error" sx={{ mt: 1 }}>
                {fileError}
              </Alert>
            )}
          </Box>
        </Box>

        {submitMutation.isError && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {submitMutation.error?.response?.data?.message ||
              'Failed to submit payment'}
          </Alert>
        )}
        {success && (
          <Alert severity="success" sx={{ mt: 2 }}>
            Payment submitted for verification. Redirecting to your payment history…
          </Alert>
        )}

        <Stack direction="row" justifyContent="flex-end" sx={{ mt: 3 }}>
          <Button
            variant="contained"
            onClick={handleSubmit}
            disabled={!canSubmit}
          >
            {submitMutation.isPending ? (
              <CircularProgress size={20} sx={{ color: 'inherit' }} />
            ) : (
              'Submit for Verification'
            )}
          </Button>
        </Stack>
      </Paper>
    </Box>
  );
}
