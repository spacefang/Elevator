import request from './request'

export const login = (data: any) => {
    return request.post('/api/auth/login', data)
}

export const getMe = () => {
    return request.get('/api/auth/me')
}
