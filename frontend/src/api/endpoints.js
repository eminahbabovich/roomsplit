import { api } from './client.js'

export const authApi = {
  register: (body) => api('/auth/register', { method: 'POST', body, auth: false }),
  login: (body) => api('/auth/login', { method: 'POST', body, auth: false }),
  me: () => api('/users/me'),
}

export const groupsApi = {
  list: () => api('/groups'),
  get: (id) => api(`/groups/${id}`),
  create: (name) => api('/groups', { method: 'POST', body: { name } }),
  join: (joinCode) => api('/groups/join', { method: 'POST', body: { joinCode } }),
}

export const expensesApi = {
  list: (groupId, { page = 0, size = 8, from, to, category } = {}) => {
    const params = new URLSearchParams({ page, size })
    if (from) params.set('from', from)
    if (to) params.set('to', to)
    if (category) params.set('category', category)
    return api(`/groups/${groupId}/expenses?${params}`)
  },
  create: (groupId, body) => api(`/groups/${groupId}/expenses`, { method: 'POST', body }),
  update: (groupId, id, body) => api(`/groups/${groupId}/expenses/${id}`, { method: 'PUT', body }),
  remove: (groupId, id) => api(`/groups/${groupId}/expenses/${id}`, { method: 'DELETE' }),
}

export const balancesApi = {
  balances: (groupId) => api(`/groups/${groupId}/balances`),
  suggestions: (groupId) => api(`/groups/${groupId}/settle-suggestions`),
  settle: (groupId, body) => api(`/groups/${groupId}/settlements`, { method: 'POST', body }),
}
