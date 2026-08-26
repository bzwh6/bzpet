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
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        // 设置 JSON 消息转换器
        rabbitTemplate.setMessageConverter(messageConverter());

        // 确认消息是否到达交换机
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            @Override
            public void confirm(@Nullable org.springframework.amqp.rabbit.connection.CorrelationData correlationData, boolean ack, @Nullable String cause) {
                if (ack) {
                    log.info("消息{}发送成功", correlationData);
                } else {
                    log.error("消息{}发送失败", correlationData, cause);
                }
            }
        });

        // 确认消息是否从交换机到达队列
        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {
            @Override
            public void returnedMessage(ReturnedMessage returned) {
                log.error("消息{}从exchange{}到达queue{}失败，原因：{}",
                        returned.getMessage(),
                        returned.getExchange(),
                        returned.getRoutingKey(),
                        returned.getReplyText());
            }
        });

        return rabbitTemplate;
    }
}
