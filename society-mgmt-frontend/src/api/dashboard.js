import api from './axios.js';

/**
 * Admin dashboard endpoints (project.md § 5.9).
 * All endpoints unwrap `{ success, data, message }` to just `data` for TanStack Query.
 */

export const getDashboardSummaryApi = () =>
  api.get('/dashboard/summary').then((r) => r.data.data);

export const getMaintenanceStatusApi = (params) =>
  api.get('/dashboard/maintenance', { params }).then((r) => r.data.data);

export const getFinancialsApi = () =>
  api.get('/dashboard/financials').then((r) => r.data.data);

export const getOutstandingApi = () =>
  api.get('/dashboard/outstanding').then((r) => r.data.data);
