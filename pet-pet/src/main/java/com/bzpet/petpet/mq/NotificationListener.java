package com.bzpet.petpet.mq;


import com.bzpet.common.constants.MqConstants;
import com.bzpet.petpet.domain.dto.NotificationDTO;
import com.bzpet.petpet.service.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationListener {
    private final INotificationService notificationService;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "notification.adption.queue",durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.PET_ADPTION_EXCHANGE,type = "topic"),
            key = MqConstants.Key.PET_ADPTION_ROUTING_KEY
    ))
    public void addNotification(NotificationDTO dto){
        //添加通知到数据库
        notificationService.addNotification(dto);
    }
}
