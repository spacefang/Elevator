import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '../api/request'
import router from '../router'

export const useAuthStore = defineStore('auth', () => {
    const token = ref(localStorage.getItem('accessToken') || '')
    const user = ref(null)

    const login = async (loginForm: any) => {
        try {
            // Real API Login
            const res: any = await request.post('/api/auth/login', loginForm)
            token.value = res.accessToken
            user.value = res.user // Assuming API returns user info, or we fetch it later

            localStorage.setItem('accessToken', token.value)
            return true
        } catch (error) {
            console.error(error)
            throw error
        }
    }

    const logout = () => {
        token.value = ''
        user.value = null
        localStorage.removeItem('accessToken')
        router.push('/login')
    }

    return {
        token,
        user,
        login,
        logout
    }
})
