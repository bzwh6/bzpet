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
│   ├── MyApplications.vue / Notifications.vue / Profile.vue
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
| PUT /upadte/info | 更新资料（后端拼写为 `upadte`） |
| PUT /upadte/password?password= | 修改密码 |
| GET /pets/list | 宠物分页列表 |
| GET /pets/detail/{id} | 宠物详情 |
| POST /pets/add | 发布宠物 |
| POST /pets/like/{id} | 点赞 / 取消 |
| POST /pets/adoption/apply | 提交领养申请 |
| GET /pets/adoption/my/list | 我的领养申请 |
| GET /notification/list | 我的通知分页 |
| GET /notification/detail/{id} | 通知详情（标记已读） |
| POST /notification/read | 全部已读 |
| DELETE /notification/read/clear | 清空已读 |
| DELETE /notification/delete/{id} | 删除通知 |

## 待后端补齐

- **「我收到的领养申请」接口**：目前后端只有「我提交的申请」，发布者无法在前端看到待处理的申请，因此前端暂无发布者审核页。
- **`/upadte` 拼写**：后端若修正为 `/update`，请同步修改 `src/api/user.js`。
