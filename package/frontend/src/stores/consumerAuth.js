import { defineStore } from 'pinia'
import http from '../api/http'

export const useConsumerAuthStore = defineStore('consumerAuth', {
  state: () => ({
    token: localStorage.getItem('consumer_token') || '',
    user: JSON.parse(localStorage.getItem('consumer_user') || 'null'),
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
  },
  actions: {
    async login(phone, password) {
      const data = await http.post('/consumer/login', {
        username: phone,
        password,
      })
      this.token = data.token
      this.user = data.user
      localStorage.setItem('consumer_token', data.token)
      localStorage.setItem('consumer_user', JSON.stringify(data.user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('consumer_token')
      localStorage.removeItem('consumer_user')
    },
  },
})
