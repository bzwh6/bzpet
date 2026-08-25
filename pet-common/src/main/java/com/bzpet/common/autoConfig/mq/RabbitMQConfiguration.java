package com.bzpet.common.autoConfig.mq;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
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
    @Resource
    private RabbitTemplate rabbitTemplate;

    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 在rabbitTemplate中配置回调函数
     */
    @PostConstruct
    public void initRabbitTemplate(){
        //确认消息是否到达exchange
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            @Override
            public void confirm(@Nullable CorrelationData correlationData, boolean ack, @Nullable String cause) {
                if (ack){
                    log.info("消息{}发送成功",correlationData);
                }else{
                    log.error("消息{}发送失败",correlationData,cause);
                }
            }
        });
        //确认消息是否从exchange到达queue
        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {
            @Override
            public void returnedMessage(ReturnedMessage returned) {
                log.error("消息{}从exchange{}到达queue{}失败，原因：{}"
                        ,returned.getMessage()
                        ,returned.getExchange()
                        ,returned.getRoutingKey()
                        ,returned.getReplyText());
            }
        });

    }
}
