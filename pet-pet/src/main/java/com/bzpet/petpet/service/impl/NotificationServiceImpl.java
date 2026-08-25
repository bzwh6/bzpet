package com.bzpet.petpet.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.bzpet.common.enums.NotificationType;
import com.bzpet.common.enums.ReadStatus;
import com.bzpet.common.utils.UserContext;
import com.bzpet.petpet.domain.dto.NotificationDTO;
import com.bzpet.petpet.domain.dto.ProcessDTO;
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
    public void rejectAdoptionApplication(ProcessDTO dto) {
        //判断申请人id是否只有一个
        if (!dto.getIsAccept()) {
            //说明发布者明确拒绝了一个申请
            //向申请人发送通知，通知被拒绝
            NotificationDTO notificationDTO = new NotificationDTO();
            notificationDTO.setType(NotificationType.NOTIFICATION_TYPE_SYSTEM);
            notificationDTO.setIsRead(ReadStatus.UNREAD);
            notificationDTO.setPetId(dto.getPetId());
            notificationDTO.setApplicantId(dto.getApplicantId().get(0));
            notificationDTO.setPublisherId(dto.getPublisherId());
            notificationDTO.setTitle(dto.getRejectReason());
            notificationDTO.setContent("很遗憾，您的领养申请已被拒绝！");
            addNotification(notificationDTO);
        } else {
            //说明发布者同意了一个申请，其他的申请被拒绝.
            //修改领养申请状态为已拒绝
            applicationService.lambdaUpdate()
                    .in(AdoptionApplication::getApplicantId, dto.getApplicantId())
                    .eq(AdoptionApplication::getPublisherId, dto.getPublisherId())
                    .eq(AdoptionApplication::getPetId, dto.getPetId())
                    .eq(AdoptionApplication::getStatus,ApplicaltionStatus.PENDING)
                    .set(AdoptionApplication::getStatus,ApplicaltionStatus.REJECTED)
                    .update();
            //向其他申请人发送通知，通知被拒绝,批量插入
            List<Notification> notificationList = new ArrayList<>();
            for (Long applicantId : dto.getApplicantId()) {
                Notification notification = new Notification();
                notification.setType(NotificationType.NOTIFICATION_TYPE_SYSTEM);
                notification.setIsRead(ReadStatus.UNREAD);
                notification.setPetId(dto.getPetId());
                notification.setApplicantId(applicantId);
                notification.setPublisherId(dto.getPublisherId());
                notification.setTitle(dto.getRejectReason());
                notification.setContent("很遗憾，您的领养申请已被拒绝！");
                notificationList.add(notification);
            }
            saveBatch(notificationList);
        }
    }


    @Override
    public void acceptAdoptionApplication(ProcessDTO dto) {
        //向领养申请人发送通知，通知领养成功
        NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setType(NotificationType.NOTIFICATION_TYPE_ADOPTED);
        notificationDTO.setIsRead(ReadStatus.UNREAD);
        notificationDTO.setPetId(dto.getPetId());
        notificationDTO.setApplicantId(dto.getApplicantId().get(0));
        notificationDTO.setPublisherId(dto.getPublisherId());
        notificationDTO.setContent("恭喜您，您的领养申请已通过！");
        addNotification(notificationDTO);
    }
}
