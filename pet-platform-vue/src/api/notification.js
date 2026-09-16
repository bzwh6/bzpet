import request from './request'

// 分页查询我的通知：{ pageNo, pageSize, type, isRead }
export const getNotificationList = (params) => request.get('/notification/list', { params })

// 通知详情（点击后标记已读，返回跳转信息 applicationId + type）
export const getNotificationDetail = (id) => request.get(`/notification/detail/${id}`)

// 全部标记已读
export const markAllRead = () => request.post('/notification/read')

// 清空已读通知
export const clearRead = () => request.delete('/notification/read/clear')

// 删除单条通知
export const deleteNotification = (id) => request.delete(`/notification/delete/${id}`)
