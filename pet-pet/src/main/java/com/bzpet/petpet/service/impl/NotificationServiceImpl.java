package com.bzpet.petpet.service.impl;



import cn.hutool.core.bean.BeanUtil;
import com.bzpet.common.enums.NotificationType;
import com.bzpet.common.enums.ReadStatus;
import com.bzpet.common.utils.UserContext;
import com.bzpet.petpet.domain.dto.NotificationDTO;
import com.bzpet.petpet.domain.po.AdoptionApplication;
import com.bzpet.petpet.domain.po.Notification;
import com.bzpet.petpet.domain.po.Pet;
import com.bzpet.petpet.enums.AdoptionStatus;
import com.bzpet.petpet.enums.ApplicaltionStatus;
import com.bzpet.petpet.mapper.NotificationMapper;
import com.bzpet.petpet.mapper.PetMapper;
import com.bzpet.petpet.service.IAdoptionApplicationService;
import com.bzpet.petpet.service.INotificationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bzpet.petpet.service.IPetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 站内通知表 服务实现类
 * </p>
 *
 * @author bzwh
 * @since 2026-08-23
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements INotificationService {
    private final IAdoptionApplicationService applicationService;
    @Override
    public void addNotification(NotificationDTO dto) {
        //把数据保存到数据库
        Notification notification = BeanUtil.copyProperties(dto, Notification.class);
        save(notification);
    }

    @Override
    @Transactional
    public void rejectAdoptionApplication(NotificationDTO dto) {
        //获取当前用户信息,是发送方ID
        Long userId = UserContext.getUserId();
        //通过领养申请人id，宠物id，更新领养申请状态为已拒绝
        int size = dto.getReceiverIds().size();
        if (size == 0)return;
        else if (size == 1) {
            //只需要拒绝一个领养申请，其他领养申请状态保持不变
            applicationService.lambdaUpdate()
                    .eq(AdoptionApplication::getApplicantId, dto.getReceiverIds().get(0))
                    .eq(AdoptionApplication::getPetId, dto.getPetId())
                    .eq(AdoptionApplication::getStatus, ApplicaltionStatus.PENDING)
                    .set(AdoptionApplication::getStatus, ApplicaltionStatus.REJECTED)
                    .update();
            Notification notification = BeanUtil.copyProperties(dto, Notification.class);
            notification.setPublisherId(dto.getReceiverIds().get(0));
            notification.setApplicantId(userId);
            notification.setPetId(dto.getPetId());
            notification.setContent("您的领养申请已被拒绝");
            notification.setType(NotificationType.NOTIFICATION_TYPE_SYSTEM);
            notification.setIsRead(ReadStatus.UNREAD);
            save(notification);
        }else {
            //全部拒绝领养申请，其他领养申请状态保持不变
            applicationService.lambdaUpdate()
                    .in(AdoptionApplication::getApplicantId, dto.getReceiverIds())
                    .eq(AdoptionApplication::getPetId, dto.getPetId())
                    .eq(AdoptionApplication::getStatus, ApplicaltionStatus.PENDING)
                    .set(AdoptionApplication::getStatus, ApplicaltionStatus.REJECTED)
                    .update();
            //批量插入通知
            //批量插入通知
            List<Notification> notifications = dto.getReceiverIds().stream()
                    .map(id -> {
                        Notification n = BeanUtil.copyProperties(dto, Notification.class);
                        n.setPublisherId(id);
                        n.setApplicantId(userId);
                        n.setPetId(dto.getPetId());
                        n.setContent("您的领养申请已被拒绝");
                        n.setType(NotificationType.NOTIFICATION_TYPE_SYSTEM);
                        n.setIsRead(ReadStatus.UNREAD);
                        return n;
                    }).collect(Collectors.toList());
            saveBatch(notifications);
        }

    }

    @Override
    public void acceptAdoptionApplication(NotificationDTO dto) {
        //获取当前用户信息,是发送方ID
        Long userId = UserContext.getUserId();
        //直接保存通知到数据库
        Notification notification = BeanUtil.copyProperties(dto, Notification.class);
        notification.setPublisherId(dto.getReceiverIds().get(0));
        notification.setApplicantId(userId);
        notification.setPetId(dto.getPetId());
        notification.setContent("恭喜您，您的领养申请已通过通过");
        notification.setType(NotificationType.NOTIFICATION_TYPE_ADOPTED);
        notification.setIsRead(ReadStatus.UNREAD);
        save(notification);
    }
}
