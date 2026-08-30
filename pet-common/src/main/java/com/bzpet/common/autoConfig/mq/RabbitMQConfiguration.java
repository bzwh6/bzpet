package com.bzpet.common.autoConfig.mq;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.Nullable;

@Slf4j
@AutoConfiguration
public class RabbitMQConfiguration {

    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 直接在创建 RabbitTemplate 时，注入 ConnectionFactory，配置好回调。
     * 彻底避免 @Resource 注入 @PostConstruct 修改导致的循环依赖。
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         ObjectProvider<MqSendResultProcessor> processorProvider) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        // 设置 JSON 消息转换器
        rabbitTemplate.setMessageConverter(messageConverter());

        // 开启 mandatory：消息路由不到队列时退回给生产者（触发 returns 回调）
        rabbitTemplate.setMandatory(true);

        // 确认消息是否到达交换机：有处理器则联动补偿表兜底，否则仅打日志
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            @Override
            public void confirm(@Nullable CorrelationData correlationData, boolean ack, @Nullable String cause) {
                MqSendResultProcessor processor = processorProvider.getIfAvailable();
                String messageId = correlationData != null ? correlationData.getId() : null;
                if (processor != null) {
                    processor.onConfirm(messageId, ack, cause);
                } else if (ack) {
                    log.info("消息发送成功: {}", messageId);
                } else {
                    log.error("消息发送失败: {} cause={}", messageId, cause);
                }
            }
        });

        // 确认消息是否从交换机到达队列（路由失败回调）
        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {
            @Override
            public void returnedMessage(ReturnedMessage returned) {
                MqSendResultProcessor processor = processorProvider.getIfAvailable();
                if (processor != null) {
                    processor.onReturn(returned);
                } else {
                    log.error("消息从exchange{}到达queue{}失败，原因：{}",
                            returned.getExchange(), returned.getRoutingKey(), returned.getReplyText());
                }
            }
        });

        return rabbitTemplate;
    }
}
