package com.bzpet.petpet.config;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通知消息的死信队列配置：消费重试耗尽后，消息进入死信队列兜底，避免被丢弃。
 */
@Configuration
public class MqDeadLetterConfig {

    public static final String NOTIFICATION_DLX = "notification.dlx.exchange";
    public static final String NOTIFICATION_DLQ = "notification.dlq.queue";
    public static final String NOTIFICATION_DLX_KEY = "notification.dead";

    @Bean
    public DirectExchange notificationDlx() {
        return new DirectExchange(NOTIFICATION_DLX, true, false);
    }

    @Bean
    public Queue notificationDlq() {
        return new Queue(NOTIFICATION_DLQ, true);
    }

    @Bean
    public Binding notificationDlqBinding() {
        return BindingBuilder.bind(notificationDlq()).to(notificationDlx()).with(NOTIFICATION_DLX_KEY);
    }

    /**
     * 消费重试耗尽后的兜底：主动拒绝且不重回队列，让消息按 x-dead-letter-exchange 进入死信队列。
     * <p>
     * 这里必须直接抛 {@link AmqpRejectAndDontRequeueException}，并且 rejectManual 传 true：
     * 容器判断要不要 nack 时只看异常的"直接类型"和这个标志（ContainerUtils.isRejectManual），
     * 不追 cause 链；一旦被包装成 ListenerExecutionFailedException，手动 ack 模式下容器
     * 既不 ack 也不 nack，消息会一直停在 unacked，死信队列永远收不到。
     * <p>
     * 注意：整个应用只能有一个 {@link MessageRecoverer} bean，Boot 通过 getIfUnique() 获取，
     * 出现多个时会返回 null 并静默退回默认实现。
     */
    @Bean
    public MessageRecoverer rejectRecoverer() {
        return (message, cause) -> {
            throw new AmqpRejectAndDontRequeueException("消费重试耗尽，转入死信队列", true, cause);
        };
    }
}
