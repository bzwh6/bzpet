package com.bzpet.petpet.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.bzpet.petpet.domain.po.MessageCompensation;
import com.bzpet.petpet.mapper.MessageCompensationMapper;
import com.bzpet.petpet.service.IMessageCompensationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * MQ消息本地补偿表 服务实现类
 * </p>
 *
 * @author bzwh
 * @since 2026-08-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageCompensationServiceImpl extends ServiceImpl<MessageCompensationMapper, MessageCompensation> implements IMessageCompensationService {

    private final RabbitTemplate rabbitTemplate;

    private static final int MAX_RETRY_COUNT = 5;

    @Override
    public Long savePendingMessage(MessageCompensation compensation) {
        compensation.setStatus(0);
        compensation.setRetryCount(0);
        compensation.setCreateTime(LocalDateTime.now());
        save(compensation);
        return compensation.getId();
    }

    @Override
    public void sendAndUpdateStatus(Long compensationId) {
        MessageCompensation compensation = getById(compensationId);
        if (compensation == null || compensation.getStatus() == 1) {
            return;
        }
        try {
            rabbitTemplate.convertAndSend(
                    compensation.getExchangeName(),
                    compensation.getRoutingKey(),
                    JSONUtil.parse(compensation.getMessageContent()),
                    new CorrelationData(compensation.getMessageId())
            );
            lambdaUpdate()
                    .eq(MessageCompensation::getId, compensationId)
                    .set(MessageCompensation::getStatus, 1)
                    .set(MessageCompensation::getUpdateTime, LocalDateTime.now())
                    .update();
            log.info("补偿消息{}发送成功", compensation.getMessageId());
        } catch (Exception e) {
            log.error("补偿消息{}发送失败：{}", compensation.getMessageId(), e.getMessage());
            lambdaUpdate()
                    .eq(MessageCompensation::getId, compensationId)
                    .set(MessageCompensation::getStatus, 2)
                    .set(MessageCompensation::getLastErrorMsg, e.getMessage())
                    .set(MessageCompensation::getUpdateTime, LocalDateTime.now())
                    .update();
        }
    }

    @Override
    public void retryFailedMessages() {
        List<MessageCompensation> list = lambdaQuery()
                .in(MessageCompensation::getStatus, 0, 2)
                .lt(MessageCompensation::getRetryCount, MAX_RETRY_COUNT)
                .list();
        if (CollUtil.isEmpty(list)) {
            return;
        }
        log.info("开始重试补偿消息，共{}条", list.size());
        for (MessageCompensation compensation : list) {
            try {
                rabbitTemplate.convertAndSend(
                        compensation.getExchangeName(),
                        compensation.getRoutingKey(),
                        JSONUtil.parse(compensation.getMessageContent()),
                        new CorrelationData(compensation.getMessageId())
                );
                lambdaUpdate()
                        .eq(MessageCompensation::getId, compensation.getId())
                        .set(MessageCompensation::getStatus, 1)
                        .set(MessageCompensation::getUpdateTime, LocalDateTime.now())
                        .update();
                log.info("补偿消息{}重试发送成功", compensation.getMessageId());
            } catch (Exception e) {
                int newRetryCount = compensation.getRetryCount() + 1;
                int newStatus = newRetryCount >= MAX_RETRY_COUNT ? 3 : 2;
                lambdaUpdate()
                        .eq(MessageCompensation::getId, compensation.getId())
                        .set(MessageCompensation::getStatus, newStatus)
                        .set(MessageCompensation::getRetryCount, newRetryCount)
                        .set(MessageCompensation::getLastErrorMsg, e.getMessage())
                        .set(MessageCompensation::getUpdateTime, LocalDateTime.now())
                        .update();
                log.error("补偿消息{}重试失败，已重试{}次：{}", compensation.getMessageId(), newRetryCount, e.getMessage());
            }
        }
    }

    /**
     * 清理本地补偿表中已发送成功的补偿消息
     */
    @Override
    public void cleanSuccessMessages() {
        LocalDateTime localDateTime = LocalDateTime.now().minusDays(7);
        lambdaUpdate()
                .eq(MessageCompensation::getStatus, 1)
                .lt(MessageCompensation::getCreateTime, localDateTime)
                .remove();
    }
}