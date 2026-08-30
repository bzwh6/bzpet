package com.bzpet.petpet.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bzpet.common.constants.RedisConstants;
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
import com.bzpet.petpet.domain.po.Pet;
import com.bzpet.petpet.domain.query.NotificationQueryDTO;
import com.bzpet.petpet.domain.vo.NotificationQueryVO;
import com.bzpet.petpet.domain.vo.NotificationVO;
import com.bzpet.petpet.enums.AdoptionStatus;
import com.bzpet.petpet.enums.ApplicaltionStatus;
import com.bzpet.petpet.mapper.NotificationMapper;
import com.bzpet.petpet.mapper.PetMapper;
import com.bzpet.petpet.service.IAdoptionApplicationService;
import com.bzpet.petpet.service.INotificationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bzpet.petpet.service.IPetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void addNotification(NotificationDTO dto) {
        // 幂等：同一 接收者+业务id+类型 只处理一次，防止 MQ 重试/重复投递造成重复通知
        String idemKey = dto.getReceiverId() + ":" + dto.getApplicationId() + ":" + dto.getType();
        Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(
                RedisConstants.KEY.MESSAGE_DONE_ID + idemKey,
                "1",
                RedisConstants.EXPIRE.NOTIFICATION_IDEM_EXPIRE,
                TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(first)) {
            log.info("通知已处理过，跳过：{}", idemKey);
            return;
        }
        try {
            //把数据保存到数据库
            Notification notification = BeanUtil.copyProperties(dto, Notification.class);
            save(notification);
        } catch (Exception e) {
            //清理缓存标记
            stringRedisTemplate.delete(RedisConstants.KEY.MESSAGE_DONE_ID + idemKey);
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional
    public void rejectAdoptionApplication(ProcessDTO dto) {
        // 幂等：同一 发布人+业务id 只处理一次，防止 MQ 重试/重复投递造成重复通知
        String idemKey = dto.getPublisherId() + ":" + dto.getApplicationId();
        Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(
                RedisConstants.KEY.MESSAGE_DONE_ID + idemKey,
                "1",
                RedisConstants.EXPIRE.NOTIFICATION_IDEM_EXPIRE,
                TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(first)) {
            log.info("通知已处理过，跳过：{}", idemKey);
            return;
        }
        try {
            //判断申请人id是否只有一个
            if (!dto.getIsAccept()) {
                //说明发布者明确拒绝了一个申请
                //向申请人发送通知，通知被拒绝
                NotificationDTO notificationDTO = new NotificationDTO();
                notificationDTO.setType(NotificationType.NOTIFICATION_TYPE_SYSTEM);
                notificationDTO.setIsRead(ReadStatus.UNREAD);
                notificationDTO.setPetId(dto.getPetId());
                notificationDTO.setApplicantId(dto.getApplicantId().get(0));
                notificationDTO.setReceiverId(dto.getApplicantId().get(0));
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
                    notification.setReceiverId(applicantId);
                    notification.setApplicationId(dto.getApplicationId());
                    notification.setPublisherId(dto.getPublisherId());
                    notification.setTitle(dto.getRejectReason());
                    notification.setContent("很遗憾，您的领养申请已被拒绝！");
                    notificationList.add(notification);
                }
                saveBatch(notificationList);
            }
        } catch (Exception e) {
            //清理缓存标记
            stringRedisTemplate.delete(RedisConstants.KEY.MESSAGE_DONE_ID + idemKey);
            throw new RuntimeException(e);
        }
    }


    @Override
    public void acceptAdoptionApplication(ProcessDTO dto) {
        // 幂等：同一 发布人+业务id 只处理一次，防止 MQ 重试/重复投递造成重复通知
        String idemKey = dto.getPublisherId() + ":" + dto.getApplicationId();
        Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(
                RedisConstants.KEY.MESSAGE_DONE_ID + idemKey,
                "1",
                RedisConstants.EXPIRE.NOTIFICATION_IDEM_EXPIRE,
                TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(first)) {
            log.info("通知已处理过，跳过：{}", idemKey);
            return;
        }
        try {
            //向领养申请人发送通知，通知领养成功
            NotificationDTO notificationDTO = new NotificationDTO();
            notificationDTO.setType(NotificationType.NOTIFICATION_TYPE_ADOPTED);
            notificationDTO.setIsRead(ReadStatus.UNREAD);
            notificationDTO.setPetId(dto.getPetId());
            notificationDTO.setApplicantId(dto.getApplicantId().get(0));
            notificationDTO.setReceiverId(dto.getApplicantId().get(0));
            notificationDTO.setApplicationId(dto.getApplicationId());
            notificationDTO.setPublisherId(dto.getPublisherId());
            notificationDTO.setContent("恭喜您，您的领养申请已通过！");
            addNotification(notificationDTO);
        } catch (Exception e) {
            //清理缓存标记
            stringRedisTemplate.delete(RedisConstants.KEY.MESSAGE_DONE_ID + idemKey);
            throw new RuntimeException(e);
        }
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
}
