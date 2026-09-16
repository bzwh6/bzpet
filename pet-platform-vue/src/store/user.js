import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(localStorage.getItem('userId') || '')

  function setLogin(newToken, newUserId) {
    token.value = newToken
    userId.value = newUserId
    localStorage.setItem('token', newToken)
    localStorage.setItem('userId', newUserId)
  }

  function clear() {
    token.value = ''
    userId.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
  }

  return { token, userId, setLogin, clear }
})
