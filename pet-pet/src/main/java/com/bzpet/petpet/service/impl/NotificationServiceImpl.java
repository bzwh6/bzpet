package com.bzpet.petpet.service.impl;



import cn.hutool.core.bean.BeanUtil;
import com.bzpet.petpet.domain.dto.NotificationDTO;
import com.bzpet.petpet.domain.po.Notification;
import com.bzpet.petpet.mapper.NotificationMapper;
import com.bzpet.petpet.service.INotificationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 站内通知表 服务实现类
 * </p>
 *
 * @author bzwh
 * @since 2026-08-23
 */
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements INotificationService {

    @Override
    public void addNotification(NotificationDTO dto) {
        //把数据保存到数据库
        Notification notification = BeanUtil.copyProperties(dto, Notification.class);
        save(notification);
    }
}
