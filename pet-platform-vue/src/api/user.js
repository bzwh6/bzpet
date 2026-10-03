import request from './request'

// 登录：{ username, password } -> token
export const login = (data) => request.post('/user/login', data)

// 退出登录：token 作为 query 参数
export const logout = (token) => request.post('/user/logout', null, { params: { token } })

// 注册：{ username, password, phone, nickname }
export const register = (data) => request.post('/user/register', data)

// 用户信息
export const getUserInfo = (id) => request.get(`/user/userInfo/${id}`)

// 批量查询用户
export const getUserList = (ids) => request.get('/user/list', { params: { ids: ids.join(',') } })

// 更新用户基础信息 { nickname, avatar }
export const updateUserInfo = (data) => request.put('/user/update/info', data)

// 更新密码：{ oldPassword, newPassword }
export const updatePassword = (data) => request.put('/user/update/password', data)

// 上传头像（FormData，字段名 file），返回 OSS 图片地址
export const uploadAvatar = (formData) =>
  request.post('/user/avatar/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
