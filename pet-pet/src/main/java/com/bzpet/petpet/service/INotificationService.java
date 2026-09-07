package com.bzpet.petpet.service;

import com.bzpet.common.domain.R;
import com.bzpet.common.domain.dto.PageDTO;
import com.bzpet.petpet.domain.dto.NotificationDTO;
import com.bzpet.petpet.domain.dto.ProcessDTO;
import com.bzpet.petpet.domain.po.Notification;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bzpet.petpet.domain.query.NotificationQueryDTO;
import com.bzpet.petpet.domain.vo.NotificationQueryVO;
import com.bzpet.petpet.domain.vo.NotificationVO;

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

    void rejectAdoptionApplication(ProcessDTO dto);

    void acceptAdoptionApplication(ProcessDTO dto);

    R<PageDTO<NotificationQueryVO>> queryList(NotificationQueryDTO dto);

    R<NotificationVO> queryDetail(Long id);

    R<Void> markAllRead();

    R<Void> deleteRead();

    R<Void> deleteNotification(Long id);

    R<Long> queryUnreadCount();
}
