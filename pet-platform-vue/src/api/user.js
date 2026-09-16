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
// 注意：后端当前路径拼写为 upadte（若后端修正为 update，请同步修改）
export const updateUserInfo = (data) => request.put('/upadte/info', data)

// 更新密码：password 作为 query 参数
export const updatePassword = (password) =>
  request.put('/upadte/password', null, { params: { password } })
