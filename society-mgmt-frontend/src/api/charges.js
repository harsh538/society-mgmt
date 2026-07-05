import api from './axios.js';

export const getChargeConfigApi = () =>
  api.get('/charge-config').then((r) => r.data.data);

export const updateChargeConfigApi = (body) =>
  api.put('/charge-config', body).then((r) => r.data.data);

export const generateChargesApi = (body) =>
  api.post('/charges/generate', body).then((r) => r.data);

export const getChargesApi = (params) =>
  api.get('/charges', { params }).then((r) => r.data.data);

export const getChargeApi = (id) =>
  api.get(`/charges/${id}`).then((r) => r.data.data);

export const voidChargeApi = (id, body) =>
  api.post(`/charges/${id}/void`, body).then((r) => r.data.data);

export const getUnitChargesApi = (unitId) =>
  api.get(`/units/${unitId}/charges`).then((r) => r.data.data);

export const getUnitOutstandingApi = (unitId) =>
  api.get(`/units/${unitId}/outstanding`).then((r) => r.data.data);
