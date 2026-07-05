import api from './axios.js';

export const getPaymentsApi = (params) =>
  api.get('/payments', { params }).then((r) => r.data.data);

export const getPendingQueueApi = (params) =>
  api.get('/payments/pending', { params }).then((r) => r.data.data);

export const getMyPaymentsApi = (params) =>
  api.get('/payments/mine', { params }).then((r) => r.data.data);

export const getPaymentApi = (id) =>
  api.get(`/payments/${id}`).then((r) => r.data.data);

export const submitPaymentApi = (formData) =>
  api
    .post('/payments', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((r) => r.data.data);

export const verifyPaymentApi = (id) =>
  api.post(`/payments/${id}/verify`).then((r) => r.data.data);

export const rejectPaymentApi = (id, body) =>
  api.post(`/payments/${id}/reject`, body).then((r) => r.data.data);

export const openProofInNewTab = async (id) => {
  const response = await api.get(`/payments/${id}/proof`);
  const url = response.data.data.url;
  window.open(url, '_blank', 'noopener');
};
