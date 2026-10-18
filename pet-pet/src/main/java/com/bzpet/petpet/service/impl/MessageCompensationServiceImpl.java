package com.bzpet.petpet.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.bzpet.common.constants.Constant;
import com.bzpet.petpet.domain.po.MessageCompensation;
import com.bzpet.petpet.mapper.MessageCompensationMapper;
import com.bzpet.petpet.service.IMessageCompensationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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

    private static final int MAX_RETRY_COUNT = 3;

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
            doSend(compensation);
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

    /**
     * 发送消息：顺带把当前线程的 requestId 写进消息头。
     * <p>
     * 投递发生在请求线程（afterCommit）时能取到 MDC；定时任务重发时线程里没有 MDC，
     * 此时不设该头，消费端会退化为不带链路 id 的日志。
     */
    private void doSend(MessageCompensation compensation) {
        String requestId = MDC.get(Constant.REQUEST_ID_HEADER);
        rabbitTemplate.convertAndSend(
                compensation.getExchangeName(),
                compensation.getRoutingKey(),
                JSONUtil.parse(compensation.getMessageContent()),
                message -> {
                    if (StrUtil.isNotBlank(requestId)) {
                        message.getMessageProperties().setHeader(Constant.REQUEST_ID_HEADER, requestId);
                    }
                    //服务端消息 id 也带上，便于和补偿表按 messageId 关联
                    message.getMessageProperties().setMessageId(compensation.getMessageId());
                    return message;
                },
                new CorrelationData(compensation.getMessageId())
        );
    }

    @Override
    public void retryFailedMessages() {
        //1. 先把重试次数已用尽、仍未发送成功的消息置为死亡(3)，等待人工介入，避免无限重试
        lambdaUpdate()
                .in(MessageCompensation::getStatus, 0, 2)
                .ge(MessageCompensation::getRetryCount, MAX_RETRY_COUNT)
                .set(MessageCompensation::getStatus, 3)
                .set(MessageCompensation::getUpdateTime, LocalDateTime.now())
                .update();

        //2. 取出还有重试次数的消息
        List<MessageCompensation> list = lambdaQuery()
                .in(MessageCompensation::getStatus, 0, 2)
                .lt(MessageCompensation::getRetryCount, MAX_RETRY_COUNT)
                .list();
        if (CollUtil.isEmpty(list)) {
            return;
        }
        log.info("开始重试补偿消息，共{}条", list.size());
        for (MessageCompensation compensation : list) {
            //本次重发即一次失败重试，失败次数 +1
            int newRetryCount = compensation.getRetryCount() + 1;
            try {
                doSend(compensation);
                lambdaUpdate()
                        .eq(MessageCompensation::getId, compensation.getId())
                        .set(MessageCompensation::getStatus, 1)
                        .set(MessageCompensation::getRetryCount, newRetryCount)
                        .set(MessageCompensation::getUpdateTime, LocalDateTime.now())
                        .update();
                log.info("补偿消息{}重试发送成功（第{}次尝试）", compensation.getMessageId(), newRetryCount);
            } catch (Exception e) {
                lambdaUpdate()
                        .eq(MessageCompensation::getId, compensation.getId())
                        .set(MessageCompensation::getStatus, 2)
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