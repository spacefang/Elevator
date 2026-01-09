import request from './request'

// Alarm Types
export interface AlarmParams {
    page?: number
    size?: number
    sort?: string
    level?: string
    status?: string
    deviceId?: string
}

export const getAlarmList = (params: AlarmParams) => {
    return request.get('/api/alarms', { params })
}

export const getAlarmDetail = (id: number | string) => {
    return request.get(`/api/alarms/${id}`)
}
