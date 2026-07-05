import api from './axios.js';

export const getReceiptsApi = (params) =>
  api.get('/receipts', { params }).then((r) => r.data.data);

export const getReceiptApi = (id) =>
  api.get(`/receipts/${id}`).then((r) => r.data.data);

export const openReceiptPdfInNewTab = async (id) => {
  const response = await api.get(`/receipts/${id}/pdf`);
  const url = response.data.data.url;
  window.open(url, '_blank', 'noopener');
};
