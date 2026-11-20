import { defineStore } from 'pinia'
import { api, getToken, setToken } from '../api/http.js'

export const useUserStore = defineStore('user', {
  state: () => ({ user: null, ready: false }),
  getters: {
    loggedIn: (s) => !!s.user,
  },
  actions: {
    /** 浏览器启动时根据本地 token 恢复登录态 */
    async restore() {
      if (this.ready) return
      if (getToken()) {
        try {
          this.user = await api.me()
        } catch {
          setToken(null)
          this.user = null
        }
      }
      this.ready = true
    },
    loginWith({ token, user }) {
      setToken(token)
      this.user = user
      this.ready = true
    },
    logout() {
      setToken(null)
      this.user = null
    },
  },
})
