# pet-platform-vue 前端

宠物领养平台前端（Vue 3 + Vite + Element Plus），对接后端微服务（经网关）。

## 技术栈

- Vue 3 + Vite
- Element Plus（UI）
- Vue Router（路由）
- Pinia（状态：token / userId）
- Axios（请求，统一拦截器）

## 运行

```bash
npm install
npm run dev          # 开发：http://localhost:5173
npm run build        # 构建
```

## 与后端对接

- 所有请求走 `/api` 前缀，由 `vite.config.js` 的 proxy 转发到**网关**（默认 `http://localhost:8083`），并去掉 `/api` 前缀。
- 启动前需保证后端已就绪：**Nacos、MySQL、Redis、RabbitMQ、网关(8083)、pet-user(8081)、pet-pet(8082)**。
- 网关需配置路由：`/user/**`、`/pets/**`、`/notification/**` 分别转发到 user / pet 服务，否则请求 404。

### 请求约定

- 登录后 token 存入 localStorage，请求拦截器自动加到 `Authorization` header（网关读取该 header）。
- 响应拦截器解包后端统一响应 `R { code, msg, data }`：`code === 200` 返回 `data`，否则弹错误提示；HTTP 401 清 token 跳登录。
- 每个响应都带请求链路 id（响应头 `requestId`，响应体 `R.requestId`），报错时把它提供给后端，即可按一次请求定位全链路日志。

## 目录结构

```
src/
├── api/            # 接口封装
│   ├── request.js  # axios 实例 + 拦截器
│   ├── user.js     # /user/**
│   ├── pet.js      # /pets/**
│   ├── adoption.js # /pets/adoption/**
│   └── notification.js
├── store/user.js   # token / userId
├── utils/jwt.js    # 从 JWT 解析 userId
├── router/index.js # 路由 + 登录守卫
├── views/          # 页面
│   ├── Login.vue / Register.vue
│   ├── PetList.vue / PetDetail.vue / PetPublish.vue
│   ├── MyApplications.vue / ReceivedApplications.vue
│   ├── Notifications.vue / Profile.vue
├── App.vue         # 顶部导航布局
└── main.js
```

## 接口映射

| 前端 | 后端 |
|---|---|
| POST /user/login | 登录，返回 token |
| POST /user/register | 注册 |
| POST /user/logout?token= | 退出登录 |
| GET /user/userInfo/{id} | 用户信息 |
| PUT /user/update/info | 更新资料（nickname / avatar） |
| PUT /user/update/password | 修改密码（body: oldPassword / newPassword） |
| POST /user/avatar/upload | 上传头像到阿里云 OSS（FormData 字段名 `file`），返回图片 URL，再由 update/info 入库 |
| GET /pets/list | 宠物分页列表 |
| GET /pets/detail/{id} | 宠物详情 |
| POST /pets/add | 发布宠物 |
| POST /pets/like/{id} | 点赞 / 取消 |
| GET /pets/like/status/{id} | 查询当前用户是否已点赞 |
| POST /pets/adoption/apply | 提交领养申请 |
| GET /pets/adoption/my/list | 我的领养申请 |
| GET /pets/adoption/received/list | 我收到的领养申请（发布者审核） |
| POST /pets/adoption/process | 处理领养申请（同意 / 拒绝） |
| GET /notification/list | 我的通知分页 |
| GET /notification/detail/{id} | 通知详情（标记已读） |
| GET /notification/unread/count | 未读通知数（顶部菜单红点） |
| POST /notification/read | 全部已读 |
| DELETE /notification/read/clear | 清空已读 |
| DELETE /notification/delete/{id} | 删除通知 |
