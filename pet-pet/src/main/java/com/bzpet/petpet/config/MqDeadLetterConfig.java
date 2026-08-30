package com.bzpet.petpet.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
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
}
