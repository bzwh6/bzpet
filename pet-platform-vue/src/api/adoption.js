import request from './request'

// 提交领养申请：{ petId, reason }
export const applyAdoption = (data) => request.post('/pets/adoption/apply', data)

// 我的领养申请列表：{ pageNo, pageSize, status }
export const getMyAdoptions = (params) => request.get('/pets/adoption/my/list', { params })

// 我收到的领养申请列表（发布人）：{ pageNo, pageSize, status }
export const getReceivedAdoptions = (params) => request.get('/pets/adoption/received/list', { params })

// 处理领养申请：{ applicationId, status, rejectReason }
export const processApplication = (data) => request.post('/pets/adoption/process', data)
