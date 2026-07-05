import api from './axios.js';

export const getExpensesApi = (params) =>
  api.get('/expenses', { params }).then(r => r.data.data);

export const getExpenseSummaryApi = (params) =>
  api.get('/expenses/summary', { params }).then(r => r.data.data);

export const getExpenseApi = (id) =>
  api.get(`/expenses/${id}`).then(r => r.data.data);

export const createExpenseApi = (expense, billFile) => {
  const form = new FormData();
  form.append('expense', new Blob([JSON.stringify(expense)], { type: 'application/json' }));
  if (billFile) form.append('bill', billFile);
  return api.post('/expenses', form).then(r => r.data.data);
};

export const updateExpenseApi = (id, expense, billFile) => {
  const form = new FormData();
  form.append('expense', new Blob([JSON.stringify(expense)], { type: 'application/json' }));
  if (billFile) form.append('bill', billFile);
  return api.put(`/expenses/${id}`, form).then(r => r.data.data);
};

export const deleteExpenseApi = (id) =>
  api.delete(`/expenses/${id}`).then(r => r.data);
