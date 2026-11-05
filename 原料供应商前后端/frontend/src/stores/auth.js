import { defineStore } from 'pinia'
import http from '../api/http'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('trace_token') || '',
    user: JSON.parse(localStorage.getItem('trace_user') || 'null'),
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    orgType: (state) => state.user?.orgType || '',
    roles: (state) => state.user?.roles || [],
    canCreateBatch: (state) =>
      state.user?.orgType === 'SUPPLIER'
      && state.user?.roles?.includes('SUPPLIER_OPERATOR'),
  },
  actions: {
    async login(username, password) {
      const data = await http.post('/auth/login', { username, password })
      this.token = data.token
      this.user = data.user
      localStorage.setItem('trace_token', data.token)
      localStorage.setItem('trace_user', JSON.stringify(data.user))
    },
    async refresh() {
      if (!this.token) return
      try {
        this.user = await http.get('/auth/me')
        localStorage.setItem('trace_user', JSON.stringify(this.user))
      } catch {
        this.logout()
      }
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('trace_token')
      localStorage.removeItem('trace_user')
    },
  },
})
