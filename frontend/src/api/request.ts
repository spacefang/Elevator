import axios from 'axios'
import router from '../router'
import { ElMessage } from 'element-plus'

const request = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '',
    timeout: 5000
})

// Request Interceptor
request.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('accessToken')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// Response Interceptor
request.interceptors.response.use(
    (response) => {
        return response.data
    },
    (error) => {
        if (error.response) {
            const { status } = error.response
            if (status === 401) {
                ElMessage.error('登录已过期，请重新登录')
                localStorage.removeItem('accessToken')
                router.push('/login')
            } else {
                ElMessage.error(error.response.data?.message || '请求失败')
            }
        }
        return Promise.reject(error)
    }
)

export default request
