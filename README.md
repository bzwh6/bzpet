# 不知宠物平台（pet-platform）

宠物领养平台：Spring Cloud 微服务后端 + Vue 3 前端。核心业务是宠物发布、领养申请/审批、站内通知与点赞。

## 整体架构

```
pet-platform-vue（Vue 3 + Vite，dev :5173）
        │  /api 前缀经 vite proxy 转发并去掉前缀
pet-getway（getway-service，:8083）── 登录鉴权 GlobalFilter
        │  Nacos 注册中心 + 配置中心，lb 负载均衡
        ├── /user/**                     → pet-user（user-service，:8081）
        └── /pets/** /notification/**    → pet-pet（pet-service，:8082）
                                             └── OpenFeign 调用 user-service 查用户信息
```

支撑设施：Nacos、MySQL、Redis（+Redisson）、RabbitMQ、xxl-job、Sentinel。

## 模块说明

| 模块 | 说明 |
|---|---|
| pet-getway | 网关：路由转发 + 登录校验（JWT 签名校验 + Redis token 双重验证，滑动续期），向下游透传 userId 请求头 |
| pet-user | 用户服务：注册（BCrypt）、登录（签发 JWT + Redis 存 token）、登出、用户信息/列表、资料与密码修改 |
| pet-pet | 宠物服务：宠物管理与分页查询、点赞、领养申请与审批、站内通知（/notification/** 也在本服务） |
| pet-common | 公共自动配置：统一响应 R、全局异常、UserContext 拦截器、RabbitTemplate（手动 ack / confirm / return）、Redisson、xxl-job、Knife4j |
| pet-jwt | JWT 生成与解析工具 |
| pet-api | OpenFeign 客户端定义（服务间调用） |
| pet-platform-vue | 前端，详见其目录内 README.md |

## 技术栈

- 后端：Java 17、Spring Boot 3.3.7、Spring Cloud 2023.0.1、Spring Cloud Alibaba 2023.0.1.0、MyBatis-Plus 3.5.12、Knife4j（OpenAPI3）
- 中间件：Nacos（注册 + 配置）、MySQL、Redis + Redisson、RabbitMQ、Sentinel、xxl-job 3.0.0
- 前端：Vue 3 + Vite + Element Plus + Pinia + Vue Router + Axios

## 核心设计

- **鉴权链路**：登录签发 JWT 并写入 Redis（3 小时）；网关校验 JWT 与 Redis 双重有效并滑动续期，非白名单路径（/user/login、/user/register）均需登录；各服务通过 UserInterceptor 把 userId 放入 UserContext（ThreadLocal）。
- **宠物详情缓存**：空值缓存防穿透、Redisson 分布式锁防击穿、随机过期时间防雪崩。
- **点赞**：Lua 脚本原子操作 Redis（Set 存点赞关系 + ZSet 存计数 + dirty 集合标记待同步），实时计数走 Redis；xxl-job syncUserLikeJob 定时对账，把点赞/取消落库到 user_like 表与 pet.likes_count。
- **领养审批通知**：本地消息表 message_compensation（事务内落表，afterCommit 后发 MQ）+ publisher confirm/return 回调联动置"待重试" + xxl-job 定时重发与清理 + 消费端死信队列兜底；审批通过用乐观锁防并发超领，并自动拒绝同宠物的其它申请；消费端手动 ack + Redis 幂等。

## 数据表

user（pet_user 库）、pet、adoption_application、notification、user_like、message_compensation（pet_pet 库）。

## 本地运行

1. **准备中间件**：Nacos、MySQL、Redis、RabbitMQ、xxl-job-admin（Sentinel dashboard 可选）。Nacos 地址在各模块bootstrap-dev.yml（当前为 192.168.150.101:8848，按实际环境修改）。
2. **Nacos 共享配置**：配置中心需存在 shared-mysql.yaml、shared-redis.yaml、shared-mybatis.yaml、shared-rabbitmq.yaml、shared-xxljob.yaml、shared-sentinel.yaml，各服务通过 bootstrap.yml 引用（数据库名取自各服务 bz.db.database）。
3. **建库建表**：按上节建 pet_user、pet_pet 两个库；仓库未提交 SQL 脚本，表结构可参照各 domain/po 实体创建。
4. **启动后端**：依次运行 PetUserApplication、PetPetApplication、PetGetwayApplication（或 mvn spring-boot:run）。
5. **启动前端**：

   ```bash
   cd pet-platform-vue
   npm install
   npm run dev     # http://localhost:5173，/api 代理到网关 8083
   ```

6. **接口文档**：pet-user / pet-pet 开启了 Knife4j（bz.swagger.enable），默认访问各服务 /doc.html。
