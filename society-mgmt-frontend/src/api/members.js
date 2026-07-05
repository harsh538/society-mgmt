import api from './axios.js';

// Each call unwraps the ApiResponse envelope (`data.data`) except for the
// delete call which returns the full envelope so the caller can read `success`.

export const getMembersApi = (params) =>
  api.get('/members', { params }).then((r) => r.data.data);

export const getMemberApi = (id) =>
  api.get(`/members/${id}`).then((r) => r.data.data);

export const createMemberApi = (body) =>
  api.post('/members', body).then((r) => r.data.data);

export const updateMemberApi = (id, body) =>
  api.put(`/members/${id}`, body).then((r) => r.data.data);

export const deleteMemberApi = (id) =>
  api.delete(`/members/${id}`).then((r) => r.data);

export const activateMemberApi = (id, body) =>
  api.post(`/members/${id}/activate`, body).then((r) => r.data.data);
