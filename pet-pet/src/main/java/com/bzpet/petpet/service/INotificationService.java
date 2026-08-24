package com.bzpet.petpet.service;

import com.bzpet.petpet.domain.dto.NotificationDTO;
import com.bzpet.petpet.domain.po.Notification;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 站内通知表 服务类
 * </p>
 *
 * @author bzwh
 * @since 2026-08-23
 */
public interface INotificationService extends IService<Notification> {

    void addNotification(NotificationDTO dto);

    void rejectAdoptionApplication(NotificationDTO dto);

    void acceptAdoptionApplication(NotificationDTO dto);
}
