import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import request from '../api/request'
import router from '../router'

export interface UserInfo {
    userId: string
    username: string
    name: string
    role: string
    region?: string | null
    city?: string | null
    permissions: string[]
}

export const useAuthStore = defineStore('auth', () => {
    const token = ref(localStorage.getItem('accessToken') || '')
    const user = ref<UserInfo | null>(null)

    const permissions = computed(() => user.value?.permissions || [])
    const role = computed(() => user.value?.role || '')

    const hasPermission = (required: string | string[]) => {
        const requiredList = Array.isArray(required) ? required : [required]
        if (requiredList.length === 0) return true
        const set = new Set(permissions.value)
        return requiredList.some((p) => set.has(p))
    }

    const login = async (loginForm: any) => {
        try {
            // Real API Login
            const res: any = await request.post('/api/auth/login', loginForm)
            token.value = res.accessToken
            user.value = res.user || null

            localStorage.setItem('accessToken', token.value)
            return true
        } catch (error) {
            console.error(error)
            throw error
        }
    }

    const loadMe = async () => {
        if (!token.value) {
            user.value = null
            return null
        }
        const res: any = await request.get('/api/auth/me')
        user.value = res || null
        return user.value
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
        role,
        permissions,
        hasPermission,
        login,
        loadMe,
        logout
    }
})
