package com.bzpet.common.constants;

public interface RedisConstants {
    interface KEY {
        // 宠物详情缓存key
        String PET_DETAIL_KEY = "pet:detail:";
        // 用户token缓存key
        String USER_TOKEN_KEY = "user:token:";
        // 消息的幂等id
        String MESSAGE_DONE_ID = "message:done:id:";
    }

    interface EXPIRE {
        // 宠物详情缓存过期时间
        long PET_DETAIL_EXPIRE = 60 * 5;
        // 空值缓存过期时间
        long NULL_EXPIRE = 10;
        // 用户token缓存过期时间3小时
        long USER_TOKEN_EXPIRE = 3 * 3600;
        // 通知幂等缓存过期时间（7天，覆盖重试窗口）
        long NOTIFICATION_IDEM_EXPIRE = 7 * 24 * 3600;
    }
}
