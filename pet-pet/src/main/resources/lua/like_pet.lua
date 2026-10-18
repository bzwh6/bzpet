-- KEYS[1] = like:pet:{petId}      (Set集合，存点赞的用户ID)
-- KEYS[2] = like:pet:count          (ZSet，存点赞总数排行榜)
-- KEYS[3] = like:dirty:pets         (Set集合，待同步到数据库的宠物id)
-- KEYS[4] = like:user:{userId}      (ZSet，该用户点赞过的宠物，score=点赞时间戳)
-- ARGV[1] = userId                 (用户ID)
-- ARGV[2] = petId                  (宠物ID)
-- ARGV[3] = timestamp              (点赞时间戳，毫秒)

-- 判断该用户是否已点赞
local isLiked = redis.call('SISMEMBER', KEYS[1], ARGV[1])

if isLiked == 1 then
    -- 已经点过，取消点赞
    redis.call('SREM', KEYS[1], ARGV[1])
    local score = redis.call('ZINCRBY', KEYS[2], -1, ARGV[2])
    if tonumber(score) < 0 then
        redis.call('ZADD', KEYS[2], 0, ARGV[2])
    end
    -- 同步移除用户维度的点赞索引：取消后"我点赞的"立刻不再出现
    redis.call('ZREM', KEYS[4], ARGV[2])
    -- 标记该宠物待同步，定时任务据此把取消落库
    redis.call('SADD', KEYS[3], ARGV[2])
    return 0
else
    -- 没点过，点赞
    redis.call('SADD', KEYS[1], ARGV[1])
    redis.call('ZINCRBY', KEYS[2], 1, ARGV[2])
    -- 记录用户维度的点赞索引，score 为点赞时间
    redis.call('ZADD', KEYS[4], ARGV[3], ARGV[2])
    -- 标记该宠物待同步
    redis.call('SADD', KEYS[3], ARGV[2])
    return 1
end
