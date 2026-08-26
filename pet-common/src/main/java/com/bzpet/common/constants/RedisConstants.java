package com.bzpet.common.constants;

public interface RedisConstants {
    interface KEY {
        // 宠物详情缓存key
        String PET_DETAIL_KEY = "pet:detail:";
    }

    interface EXPIRE {
        // 宠物详情缓存过期时间
        long PET_DETAIL_EXPIRE = 60 * 5;
        // 空值缓存过期时间
        long NULL_EXPIRE = 10;
    }
}
