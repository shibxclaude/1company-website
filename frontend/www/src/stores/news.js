import { defineStore } from 'pinia'

export const useNewsStore = defineStore('news', {
  state: () => ({ list: [], total: 0, page: 1, loaded: false, details: {} }),
})
