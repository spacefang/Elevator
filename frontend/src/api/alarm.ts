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

export const processAlarm = (id: number | string, data?: { note?: string }) => {
    return request.post(`/api/alarms/${id}/process`, data || {})
}

export const closeAlarm = (id: number | string, data: { note: string }) => {
    return request.post(`/api/alarms/${id}/close`, data)
}

export const transferAlarm = (id: number | string, data?: { note?: string }) => {
    return request.post(`/api/alarms/${id}/transfer`, data || {})
}

export const superviseAlarm = (id: number | string, data?: { note?: string }) => {
    return request.post(`/api/alarms/${id}/supervise`, data || {})
}
