package com.bzpet.common.autoConfig.mq;

import org.springframework.amqp.core.ReturnedMessage;

/**
 * 消息发送结果处理器（可选实现）。
 * <p>
 * 供 RabbitTemplate 的 publisher confirm / returns 回调委托，用于联动本地补偿表做兜底：
 * 当交换机拒收（confirm ack=false）或路由失败（returns）时，把补偿表对应消息置回"待重试"，
 * 由定时任务重发。若服务未提供实现类，回调仅打日志。
 */
public interface MqSendResultProcessor {

    /**
     * 交换机确认回调。
     *
     * @param messageId 发送时 CorrelationData 携带的消息ID，可能为 null
     * @param ack       是否被交换机确认接收
     * @param cause     失败原因（ack=false 时有值）
     */
    void onConfirm(String messageId, boolean ack, String cause);

    /**
     * 路由失败回调：消息从交换机未能路由到任何队列。
     *
     * @param returned 被退回的消息
     */
    void onReturn(ReturnedMessage returned);
}
