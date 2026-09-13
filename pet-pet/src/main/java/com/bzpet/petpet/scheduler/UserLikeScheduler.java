package com.bzpet.petpet.scheduler;

import com.bzpet.petpet.service.IPetService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserLikeScheduler {

    private final IPetService petService;

    /**
     * 定时把 Redis 中的点赞关系与总数对账落库（持久化点赞）。
     * 处理"Redis 有 DB 无 → 新增"和"DB 有 Redis 无 → 删除（取消点赞）"。
     */
    @XxlJob("syncUserLikeJob")
    public void syncUserLike() {
        try {
            petService.saveLikeCount();
        } catch (Exception e) {
            log.error("同步点赞数据定时任务执行异常：{}", e.getMessage(), e);
        }
    }
}
