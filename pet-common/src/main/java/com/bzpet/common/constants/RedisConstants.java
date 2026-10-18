package com.bzpet.common.constants;

public interface RedisConstants {
    interface KEY {
        // 宠物详情缓存key
        String PET_DETAIL_KEY = "pet:detail:";
        // 用户token缓存key
        String USER_TOKEN_KEY = "user:token:";
        // 点赞宠物的缓存key,记录点赞的用户id
        String LIKE_PET_KEY = "like:pet:";
        // 点赞宠物的缓存key,记录点赞数
        String LIKE_PET_COUNT_KEY = "like:pet:count";
        // 待同步到数据库的宠物id集合（点赞/取消时标记，定时任务据此对账落库）
        String LIKE_DIRTY_PETS_KEY = "like:dirty:pets";
        // 用户点赞过的宠物（ZSet：member=petId，score=点赞时间戳），供"我点赞的"按时间倒序查询
        String LIKE_USER_KEY = "like:user:";
    }

    interface EXPIRE {
        // 宠物详情缓存过期时间
        long PET_DETAIL_EXPIRE = 60 * 5;
        // 空值缓存过期时间
        long NULL_EXPIRE = 10;
        // 用户token缓存过期时间3小时
        long USER_TOKEN_EXPIRE = 3 * 3600;
    }
}
