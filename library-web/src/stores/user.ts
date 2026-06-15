import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('accessToken') || '')
  const role = ref<string>(localStorage.getItem('role') || 'READER')
  const username = ref<string>(localStorage.getItem('username') || '')
  const isLoggedIn = computed(() => !!token.value)

  function login(accessToken: string, refreshToken: string, user: { username: string; role: string }) {
    token.value = accessToken
    role.value = user.role
    username.value = user.username
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshToken)
    localStorage.setItem('role', user.role)
    localStorage.setItem('username', user.username)
  }

  function logout() {
    token.value = ''
    role.value = 'READER'
    username.value = ''
    localStorage.clear()
  }

  function hasRole(r: string): boolean { return role.value === r }

  return { token, role, username, isLoggedIn, login, logout, hasRole }
})
