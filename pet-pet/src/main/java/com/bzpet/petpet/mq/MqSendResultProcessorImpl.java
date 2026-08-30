package com.bzpet.petpet.mq;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bzpet.common.autoConfig.mq.MqSendResultProcessor;
import com.bzpet.petpet.domain.po.MessageCompensation;
import com.bzpet.petpet.mapper.MessageCompensationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 消息发送结果处理器实现：confirm / returns 回调联动本地补偿表。
 * <p>
 * 当交换机拒收（confirm ack=false）或路由失败（returns）时，把补偿表对应消息置为"待重试"(2)，
 * 由 {@code MessageCompensationScheduler} 定时重发，弥补补偿表只处理同步异常、无法感知异步确认的缺口。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqSendResultProcessorImpl implements MqSendResultProcessor {

    private final MessageCompensationMapper compensationMapper;

    @Override
    public void onConfirm(String messageId, boolean ack, String cause) {
        if (ack) {
            log.info("消息确认成功: {}", messageId);
        } else {
            log.error("消息确认失败(交换机拒收): {} cause={}", messageId, cause);
            toRetry(messageId, "交换机拒收: " + cause);
        }
    }

    @Override
    public void onReturn(ReturnedMessage returned) {
        String messageId = returned.getMessage().getMessageProperties().getCorrelationId();
        log.error("消息路由失败：exchange={} key={} reply={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText());
        toRetry(messageId, "路由失败: " + returned.getReplyText());
    }

    /** 把补偿表中对应消息置为"待重试"(2)，交给定时任务重发 */
    private void toRetry(String messageId, String error) {
        if (messageId == null || messageId.isEmpty()) {
            return;
        }
        compensationMapper.update(null, new LambdaUpdateWrapper<MessageCompensation>()
                .eq(MessageCompensation::getMessageId, messageId)
                .set(MessageCompensation::getStatus, 2)
                .set(MessageCompensation::getLastErrorMsg, error)
                .set(MessageCompensation::getUpdateTime, LocalDateTime.now()));
    }
}
