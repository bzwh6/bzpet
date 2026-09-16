import request from './request'

// 分页查询宠物列表：{ pageNo, pageSize, age, gender, adoptionStatus }
export const getPetList = (params) => request.get('/pets/list', { params })

// 宠物详情
export const getPetDetail = (id) => request.get(`/pets/detail/${id}`)

// 新增宠物：{ petName, breed, age, gender, description, imageUrl }
export const addPet = (data) => request.post('/pets/add', data)

// 更新宠物
export const updatePet = (id, data) => request.post(`/pets/update/${id}`, data)

// 删除宠物
export const deletePet = (id) => request.delete(`/pets/delete/${id}`)

// 点赞 / 取消点赞
export const likePet = (id) => request.post(`/pets/like/${id}`)

// 查询当前用户是否已点赞
export const isLiked = (id) => request.get(`/pets/like/status/${id}`)
