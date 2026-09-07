package com.bzpet.petpet.scheduler;

import com.bzpet.petpet.service.IMessageCompensationService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageCompensationScheduler {

    private final IMessageCompensationService messageCompensationService;

    @XxlJob("retryCompensationMessagesJob")
    public void retryCompensationMessages() {
        try {
            messageCompensationService.retryFailedMessages();
        } catch (Exception e) {
            log.error("补偿消息定时任务执行异常：{}", e.getMessage(), e);
        }
    }

    @XxlJob("cleanCompensationMessagesJob")
    public void cleanCompensationMessages() {
        try {
            messageCompensationService.cleanSuccessMessages();
        } catch (Exception e) {
            log.error("清理补偿消息定时任务执行异常：{}", e.getMessage(), e);
        }
    }
}