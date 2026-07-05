import api from './axios.js';

// Each call unwraps the ApiResponse envelope so callers receive the payload
// (`data.data`) directly. Logout returns the whole envelope so callers can read
// the success flag.

export const loginApi = ({ phone, password }) =>
  api.post('/auth/login', { phone, password }).then((r) => r.data.data);

export const refreshApi = ({ refreshToken }) =>
  api.post('/auth/refresh', { refreshToken }).then((r) => r.data.data);

export const logoutApi = ({ refreshToken }) =>
  api.post('/auth/logout', { refreshToken }).then((r) => r.data);

export const getMeApi = () => api.get('/auth/me').then((r) => r.data.data);
