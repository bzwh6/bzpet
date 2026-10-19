-- ============================================================
-- 建库建表脚本（仅结构，不含数据）
-- 来源：从本地 MySQL 8.0.45 用 DataGrip 导出的 DDL
-- 执行：mysql -uroot -p < sql/00_schema.sql
-- 说明：两个库分别归属两个服务，字符集统一 utf8mb4（MySQL 8 默认）
-- ============================================================

CREATE DATABASE IF NOT EXISTS pet_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS pet_pet DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- ============================================================
-- pet_user：用户服务
-- ============================================================
USE pet_user;

create table user
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    username    varchar(50)                           not null comment '用户名',
    password    varchar(100)                          not null comment '加密密码',
    phone       varchar(20)                           null comment '手机号',
    nickname    varchar(50) default '宠物用户'        null comment '昵称',
    avatar      varchar(255)                          null comment '头像URL',
    email       varchar(100)                          null comment '邮箱',
    status      tinyint(1)  default 0                 null comment '状态: 0正常 1禁用',
    create_time datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_email
        unique (email),
    constraint uk_phone
        unique (phone),
    constraint uk_username
        unique (username)
)
    comment '用户表';

-- ============================================================
-- pet_pet：宠物服务
-- ============================================================
USE pet_pet;

create table pet
(
    id           bigint auto_increment comment '主键ID'
        primary key,
    pet_name     varchar(50)                          not null comment '宠物昵称',
    breed        tinyint                              null comment '品种，对应 PetBreed',
    age          int                                  null comment '年龄',
    gender       tinyint(1) default 0                 null comment '性别 0公 1母',
    description  varchar(255)                         null comment '性格描述',
    image_url    varchar(255)                         null comment '图片链接',
    publisher_id bigint                               null comment '发布人ID',
    status       tinyint(1) default 0                 null comment '状态: 0待领养 1已领养 2下架',
    create_time  datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time  datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    likes_count  int        default 0                 not null comment '点赞数，由 xxl-job 对账更新'
)
    comment '宠物表';

create table adoption_application
(
    id            bigint auto_increment comment '主键ID'
        primary key,
    pet_id        bigint                               not null comment '宠物ID',
    applicant_id  bigint                               not null comment '申请人ID',
    publisher_id  bigint                               not null comment '宠物发布人ID',
    reason        varchar(255)                         null comment '领养理由',
    status        tinyint(1) default 0                 not null comment '状态: 0待审核 1已同意 2已拒绝',
    create_time   datetime   default CURRENT_TIMESTAMP null comment '申请时间',
    audit_time    datetime                             null comment '审核时间',
    reject_reason varchar(255)                         null comment '拒绝原因',
    constraint uk_pet_applicant
        unique (pet_id, applicant_id, status)
)
    comment '宠物领养申请表';

create index idx_applicant_id
    on adoption_application (applicant_id)
    comment '用户申请记录索引';

create index idx_pet_id
    on adoption_application (pet_id)
    comment '宠物查询索引';

create table notification
(
    id             bigint auto_increment comment '主键ID'
        primary key,
    publisher_id   bigint                               not null comment '发布人ID（接收者）',
    applicant_id   bigint                               not null comment '申请人ID（触发者）',
    pet_id         bigint                               not null comment '宠物ID（方便点击跳转详情）',
    title          varchar(255)                         null comment '标题（即申请人的申请内容）',
    content        varchar(500)                         null comment '通知内容（固定的业务模板）',
    type           tinyint(1) default 0                 null comment '通知类型：0领养申请 1审核通过 2系统通知',
    is_read        tinyint(1) default 0                 null comment '已读状态：0未读 1已读',
    create_time    datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    application_id bigint                               null comment '关联的申请ID',
    receiver_id    bigint                               null comment '接收者id',
    idem_key       varchar(100)                         null comment '幂等键：receiverId:applicationId:type',
    constraint uk_idem_key
        unique (idem_key)
)
    comment '站内通知表';

create index idx_publisher_id
    on notification (publisher_id)
    comment '接收人查询索引';

create table user_like
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    user_id     bigint                             not null comment '点赞用户ID',
    pet_id      bigint                             not null comment '被点赞宠物ID',
    create_time datetime default CURRENT_TIMESTAMP null comment '点赞时间',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_user_pet
        unique (user_id, pet_id)
)
    comment '宠物点赞记录表';

create table message_compensation
(
    id              bigint auto_increment comment '主键ID'
        primary key,
    business_type   varchar(50)                        not null comment '业务类型：如ADOPTION_APPROVE',
    application_id  bigint                             null comment '业务ID（如申请单ID）',
    receiver_ids    json                               null comment '接收者ID集合',
    exchange_name   varchar(100)                       not null comment '交换机名称',
    routing_key     varchar(100)                       not null comment '路由键',
    message_id      varchar(64)                        not null comment '全局唯一消息ID（复合ID或UUID）',
    message_content text                               not null comment '消息体内容（JSON）',
    status          tinyint  default 0                 not null comment '状态：0待发送，1发送成功，2发送失败待重试，3死亡（人工介入）',
    retry_count     int      default 0                 not null comment '已重试次数',
    last_error_msg  varchar(500)                       null comment '最后一次失败原因',
    create_time     datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time     datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment 'MQ消息本地补偿表';

create index idx_message_id
    on message_compensation (message_id);

create index idx_status
    on message_compensation (status);
