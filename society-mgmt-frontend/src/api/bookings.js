import api from './axios.js';

export const getBookingsApi = (params) =>
  api.get('/bookings', { params }).then(r => r.data.data);

export const getMyBookingsApi = (params) =>
  api.get('/bookings/mine', { params }).then(r => r.data.data);

export const getAvailabilityApi = (year, month) =>
  api.get('/bookings/availability', { params: { year, month } }).then(r => r.data.data);

export const requestBookingApi = (data) =>
  api.post('/bookings', data).then(r => r.data.data);

export const approveBookingApi = (id, data) =>
  api.post(`/bookings/${id}/approve`, data).then(r => r.data.data);

export const rejectBookingApi = (id, data) =>
  api.post(`/bookings/${id}/reject`, data).then(r => r.data.data);

export const cancelBookingApi = (id) =>
  api.post(`/bookings/${id}/cancel`).then(r => r.data.data);
