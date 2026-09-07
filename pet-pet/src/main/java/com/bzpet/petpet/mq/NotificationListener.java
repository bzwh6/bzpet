package com.bzpet.petpet.mq;


import com.bzpet.common.constants.MqConstants;
import com.bzpet.common.constants.RedisConstants;
import com.bzpet.petpet.config.MqDeadLetterConfig;
import com.bzpet.petpet.domain.dto.NotificationDTO;
import com.bzpet.petpet.domain.dto.ProcessDTO;
import com.bzpet.petpet.service.INotificationService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Argument;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class NotificationListener {
    private final INotificationService notificationService;

    /**
     * 监听通知队列
     * @param dto
     * @param channel
     * @param deliveryTag
     * @throws IOException
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "notification.adoption.queue", durable = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = MqDeadLetterConfig.NOTIFICATION_DLX),
                            @Argument(name = "x-dead-letter-routing-key", value = MqDeadLetterConfig.NOTIFICATION_DLX_KEY)
                    }),
            exchange = @Exchange(name = MqConstants.Exchange.PET_ADPTION_EXCHANGE, type = "topic"),
            key = MqConstants.Key.PET_ADPTION_ROUTING_KEY
    ))
    public void addNotification(NotificationDTO dto, Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) Long deliveryTag) throws IOException {
        try {
            //添加通知到数据库
            notificationService.addNotification(dto);
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "notification.accept.adoption.queue", durable = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = MqDeadLetterConfig.NOTIFICATION_DLX),
                            @Argument(name = "x-dead-letter-routing-key", value = MqDeadLetterConfig.NOTIFICATION_DLX_KEY)
                    }),
            exchange = @Exchange(name = MqConstants.Exchange.PET_ADPTION_EXCHANGE, type = "topic"),
            key = MqConstants.Key.PET_ADPTION_ACCEPT_ROUTING_KEY
    ))
    public void acceptAdoptionApplication(ProcessDTO dto, Channel channel,
                                          @Header(AmqpHeaders.DELIVERY_TAG) Long deliveryTag) throws IOException  {
        try {
            //通过领养申请，更新领养申请状态为已通过
            notificationService.acceptAdoptionApplication(dto);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "notification.reject.adoption.queue", durable = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = MqDeadLetterConfig.NOTIFICATION_DLX),
                            @Argument(name = "x-dead-letter-routing-key", value = MqDeadLetterConfig.NOTIFICATION_DLX_KEY)
                    }),
            exchange = @Exchange(name = MqConstants.Exchange.PET_ADPTION_EXCHANGE, type = "topic"),
            key = MqConstants.Key.PET_ADPTION_REJECT_ROUTING_KEY
    ))
    public void rejectAdoptionApplication(ProcessDTO dto, Channel channel,
                                          @Header(AmqpHeaders.DELIVERY_TAG) Long deliveryTag) throws IOException {
        try {
            //拒绝领养申请，更新领养申请状态为已拒绝
            notificationService.rejectAdoptionApplication(dto);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
