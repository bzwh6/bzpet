package com.bzpet.petpet.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bzpet.common.domain.R;
import com.bzpet.common.domain.dto.PageDTO;
import com.bzpet.common.enums.NotificationType;
import com.bzpet.common.enums.ReadStatus;
import com.bzpet.common.exception.BadException;
import com.bzpet.common.utils.UserContext;
import com.bzpet.petpet.domain.dto.NotificationDTO;
import com.bzpet.petpet.domain.dto.ProcessDTO;
import com.bzpet.petpet.domain.po.AdoptionApplication;
import com.bzpet.petpet.domain.po.Notification;
import com.bzpet.petpet.domain.query.NotificationQueryDTO;
import com.bzpet.petpet.domain.vo.NotificationQueryVO;
import com.bzpet.petpet.domain.vo.NotificationVO;
import com.bzpet.petpet.enums.ApplicationStatus;
import com.bzpet.petpet.mapper.NotificationMapper;
import com.bzpet.petpet.service.IAdoptionApplicationService;
import com.bzpet.petpet.service.INotificationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements INotificationService {
    private final IAdoptionApplicationService applicationService;

    @Override
    public void addNotification(NotificationDTO dto) {
        //幂等：幂等键与通知落在同一行，靠唯一索引让"占位 + 落库"成为一次原子写。
        //MQ 重复投递时 INSERT 会撞唯一键，捕获后视为已处理，不再依赖 Redis。
        Notification notification = BeanUtil.copyProperties(dto, Notification.class);
        notification.setIdemKey(idemKeyOf(dto.getReceiverId(), dto.getApplicationId(), dto.getType()));
        try {
            save(notification);
        } catch (DuplicateKeyException e) {
            log.info("通知已处理过，跳过：{}", notification.getIdemKey());
        }
    }

    /** 幂等键：接收者 + 业务id + 类型，与 notification.idem_key 唯一索引配合使用 */
    private String idemKeyOf(Long receiverId, Long applicationId, Object type) {
        return receiverId + ":" + applicationId + ":" + type;
    }

    @Override
    @Transactional
    public void rejectAdoptionApplication(ProcessDTO dto) {
        //重复投递由通知表的幂等键（唯一索引）拦截；申请状态是条件更新，天然幂等
        if (!dto.getIsAccept()) {
            //说明发布者明确拒绝了一个申请
            //先把该申请落库为已拒绝（条件更新，重复消费/重试天然幂等）
            if (dto.getApplicationId() != null) {
                applicationService.lambdaUpdate()
                        .eq(AdoptionApplication::getId, dto.getApplicationId())
                        .eq(AdoptionApplication::getStatus, ApplicationStatus.PENDING)
                        .set(AdoptionApplication::getStatus, ApplicationStatus.REJECTED)
                        .update();
            }
            //向申请人发送通知，通知被拒绝
            Long applicantId = dto.getApplicantId().get(0);
            NotificationDTO notificationDTO = new NotificationDTO();
            notificationDTO.setType(NotificationType.NOTIFICATION_TYPE_SYSTEM);
            notificationDTO.setIsRead(ReadStatus.UNREAD);
            notificationDTO.setPetId(dto.getPetId());
            notificationDTO.setApplicantId(applicantId);
            notificationDTO.setReceiverId(applicantId);
            notificationDTO.setApplicationId(dto.getApplicationId());
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
                    .eq(AdoptionApplication::getStatus, ApplicationStatus.PENDING)
                    .set(AdoptionApplication::getStatus, ApplicationStatus.REJECTED)
                    .update();
            //向其他申请人发送通知，通知被拒绝
            //逐条写入而不是 saveBatch：单条撞唯一键只会跳过它自己，不会让整批都插不进去
            for (Long applicantId : dto.getApplicantId()) {
                NotificationDTO notificationDTO = new NotificationDTO();
                notificationDTO.setType(NotificationType.NOTIFICATION_TYPE_SYSTEM);
                notificationDTO.setIsRead(ReadStatus.UNREAD);
                notificationDTO.setPetId(dto.getPetId());
                notificationDTO.setApplicantId(applicantId);
                notificationDTO.setReceiverId(applicantId);
                notificationDTO.setApplicationId(dto.getApplicationId());
                notificationDTO.setPublisherId(dto.getPublisherId());
                notificationDTO.setTitle(dto.getRejectReason());
                notificationDTO.setContent("很遗憾，您的领养申请已被拒绝！");
                addNotification(notificationDTO);
            }
        }
    }


    @Override
    public void acceptAdoptionApplication(ProcessDTO dto) {
        //通知幂等由 addNotification 的唯一索引保证，不再需要额外的占位
        Long applicantId = dto.getApplicantId().get(0);
        NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setType(NotificationType.NOTIFICATION_TYPE_ADOPTED);
        notificationDTO.setIsRead(ReadStatus.UNREAD);
        notificationDTO.setPetId(dto.getPetId());
        notificationDTO.setApplicantId(applicantId);
        notificationDTO.setReceiverId(applicantId);
        notificationDTO.setApplicationId(dto.getApplicationId());
        notificationDTO.setPublisherId(dto.getPublisherId());
        notificationDTO.setContent("恭喜您，您的领养申请已通过！");
        addNotification(notificationDTO);
    }

    @Override
    public R<PageDTO<NotificationQueryVO>> queryList(NotificationQueryDTO dto) {
        //根据查询参数查询我的通知列表（接收者）
        Long userId = UserContext.getUserId();
        Page<Notification> page = lambdaQuery()
                .eq(Notification::getReceiverId, userId)
                .eq(dto.getType() != null, Notification::getType, dto.getType())
                .eq(dto.getIsRead() != null, Notification::getIsRead, dto.getIsRead())
                .orderByDesc(Notification::getCreateTime)
                .page(new Page<>(dto.getPageNo(), dto.getPageSize()));
        PageDTO<NotificationQueryVO> pageDTO = new PageDTO<>();
        pageDTO.setTotal(page.getTotal());
        pageDTO.setPages(page.getPages());
        List<NotificationQueryVO> voList = page.getRecords().stream()
                .map(notification -> BeanUtil.copyProperties(notification, NotificationQueryVO.class))
                .collect(Collectors.toList());
        pageDTO.setList(voList);
        return R.ok(pageDTO);
    }

    @Override
    public R<NotificationVO> queryDetail(Long id) {
        //查询通知，并校验必须是当前用户接收的
        Long userId = UserContext.getUserId();
        Notification notification = lambdaQuery()
                .eq(Notification::getId, id)
                .eq(Notification::getReceiverId, userId)
                .one();
        if (notification == null) {
            throw new BadException("通知不存在");
        }
        //点击进入通知，修改为已读（条件更新，天然幂等）
        lambdaUpdate()
                .eq(Notification::getId, id)
                .eq(Notification::getIsRead, ReadStatus.UNREAD)
                .set(Notification::getIsRead, ReadStatus.READ)
                .update();
        //返回跳转信息
        NotificationVO vo = new NotificationVO();
        vo.setApplicationId(notification.getApplicationId());
        vo.setIdemKey(notification.getIdemKey());
        vo.setType(notification.getType());
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setIsRead(ReadStatus.READ);
        vo.setCreateTime(notification.getCreateTime());
        return R.ok(vo);
    }

    @Override
    public R<Void> markAllRead() {
        //把当前用户接收的所有未读通知置为已读
        Long userId = UserContext.getUserId();
        lambdaUpdate()
                .eq(Notification::getReceiverId, userId)
                .eq(Notification::getIsRead, ReadStatus.UNREAD)
                .set(Notification::getIsRead, ReadStatus.READ)
                .update();
        return R.ok();
    }

    @Override
    public R<Void> deleteRead() {
        //删除当前用户的所有已读通知
        Long userId = UserContext.getUserId();
        lambdaUpdate()
                .eq(Notification::getReceiverId, userId)
                .eq(Notification::getIsRead, ReadStatus.READ)
                .remove();
        return R.ok();
    }

    @Override
    public R<Void> deleteNotification(Long id) {
        //查询通知，并校验必须是当前用户接收的
        Long userId = UserContext.getUserId();
        Notification notification = lambdaQuery()
                .eq(Notification::getId, id)
                .eq(Notification::getReceiverId, userId)
                .one();
        if (notification == null) {
            throw new BadException("通知不存在");
        }
        //判断通知是否已读
        if (notification.getIsRead() != ReadStatus.READ) {
            throw new BadException("未读通知不能删除");
        }
        removeById(id);
        return R.ok();
    }

    @Override
    public R<Long> queryUnreadCount() {
        //查询当前用户接收的所有未读通知数量
        Long userId = UserContext.getUserId();
        Long count = lambdaQuery()
                .eq(Notification::getReceiverId, userId)
                .eq(Notification::getIsRead, ReadStatus.UNREAD)
                .count();
        return R.ok(count);
    }
}
