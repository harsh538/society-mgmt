import api from './axios.js';

export const getUnitsApi = (params) =>
  api.get('/units', { params }).then((r) => r.data.data);

export const getUnitApi = (id) =>
  api.get(`/units/${id}`).then((r) => r.data.data);

export const createUnitApi = (body) =>
  api.post('/units', body).then((r) => r.data.data);

export const updateUnitApi = (id, body) =>
  api.put(`/units/${id}`, body).then((r) => r.data.data);

export const deleteUnitApi = (id) =>
  api.delete(`/units/${id}`).then((r) => r.data);

export const linkMemberApi = (unitId, body) =>
  api.post(`/units/${unitId}/members`, body).then((r) => r.data.data);

export const unlinkMemberApi = (unitId, memberId) =>
  api.delete(`/units/${unitId}/members/${memberId}`).then((r) => r.data);
