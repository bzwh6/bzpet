package com.bzpet.petpet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bzpet.common.constants.MqConstants;
import com.bzpet.common.domain.R;
import com.bzpet.common.domain.dto.PageDTO;
import com.bzpet.common.enums.NotificationType;
import com.bzpet.common.exception.BadException;
import com.bzpet.common.utils.UserContext;
import com.bzpet.petapi.client.user.UserClient;
import com.bzpet.petapi.domain.vo.UserVO;
import com.bzpet.petpet.domain.dto.AdoptionApplyDTO;
import com.bzpet.petpet.domain.dto.NotificationDTO;
import com.bzpet.petpet.domain.dto.ProcessApplicationDTO;
import com.bzpet.petpet.domain.dto.ProcessDTO;
import com.bzpet.petpet.domain.po.AdoptionApplication;
import com.bzpet.petpet.domain.po.MessageCompensation;
import com.bzpet.petpet.domain.po.Pet;
import com.bzpet.petpet.domain.query.AdoptionApplyQueryDTO;
import com.bzpet.petpet.domain.vo.AdoptionApplyVO;
import com.bzpet.petpet.enums.AdoptionStatus;
import com.bzpet.petpet.enums.ApplicaltionStatus;
import com.bzpet.petpet.enums.BusinessType;
import com.bzpet.petpet.mapper.AdoptionApplicationMapper;
import com.bzpet.petpet.service.IAdoptionApplicationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bzpet.petpet.service.IPetService;
import com.bzpet.petpet.service.IMessageCompensationService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 宠物领养申请表 服务实现类
 * </p>
 *
 * @author bzwh
 * @since 2026-08-23
 */
@Service
@RequiredArgsConstructor
public class AdoptionApplicationServiceImpl extends ServiceImpl<AdoptionApplicationMapper, AdoptionApplication> implements IAdoptionApplicationService {
    private final IPetService petService;
    private final RabbitTemplate rabbitTemplate;
    private final IMessageCompensationService messageCompensationService;
    private final UserClient userClient;

    @Override
    public R<String> applyAdoption(AdoptionApplyDTO applyDTO) {
        //获取当前用户ID,申请人
        Long userId = UserContext.getUserId();
        //获取当前宠物信息
        Pet pet = petService.getById(applyDTO.getPetId());
        if (pet == null) {
            throw new BadException("宠物不存在");
        }
        //判断宠物状态是否为待领养
        if (pet.getStatus() != AdoptionStatus.AVAILABLE) {
            throw new BadException("宠物状态不是待领养");
        }
        //判断申请人和领养人是否是同一个人
        if (userId.equals(pet.getPublisherId())) {
            throw new BadException("申请人和领养人不能是同一个人");
        }
        //创建领养申请
        AdoptionApplication adoptionApplication = new AdoptionApplication();
        adoptionApplication.setReason(applyDTO.getReason());
        adoptionApplication.setApplicantId(userId);
        adoptionApplication.setPublisherId(pet.getPublisherId());
        adoptionApplication.setStatus(ApplicaltionStatus.PENDING);
        adoptionApplication.setPetId(applyDTO.getPetId());
        try {
            //保存领养申请
            save(adoptionApplication);
        } catch (Exception e) {
            throw new BadException("不可重复提交申请！");
        }
        //向发布人发送通知
        NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setPublisherId(pet.getPublisherId());
        notificationDTO.setApplicantId(userId);
        notificationDTO.setReceiverId(pet.getPublisherId());
        notificationDTO.setApplicationId(adoptionApplication.getId());
        notificationDTO.setPetId(applyDTO.getPetId());
        notificationDTO.setTitle(applyDTO.getReason());
        notificationDTO.setContent("您有新的领养申请");
        notificationDTO.setType(NotificationType.NOTIFICATION_TYPE_ADOPTION);
        rabbitTemplate.convertAndSend(
                MqConstants.Exchange.PET_ADPTION_EXCHANGE,
                MqConstants.Key.PET_ADPTION_ROUTING_KEY,
                notificationDTO
        );
        return R.ok("领养申请成功");
    }

    @Override
    public R<PageDTO<AdoptionApplyVO>> myAdoptionList(AdoptionApplyQueryDTO queryDTO) {
        //获取我的id，申请人
        Long userId = UserContext.getUserId();
        //分页查询我的领养申请列表
        Page<AdoptionApplication> page = lambdaQuery()
                .eq(AdoptionApplication::getApplicantId, userId)
                .eq(queryDTO.getStatus() != null, AdoptionApplication::getStatus, queryDTO.getStatus())
                .orderByDesc(AdoptionApplication::getCreateTime)
                .page(new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize()));
        List<AdoptionApplication> records = page.getRecords();
        PageDTO<AdoptionApplyVO> pageDTO = new PageDTO<>();
        pageDTO.setTotal(page.getTotal());
        pageDTO.setPages(page.getPages());
        if (CollUtil.isEmpty(records)) {
            return R.ok(pageDTO);
        }
        //查询发布人昵称，头像信息
        List<Long> publisherIds = records.stream()
                .map(AdoptionApplication::getPublisherId).collect(Collectors.toList());
        List<UserVO> userVOS = userClient.userInfoList(publisherIds).getData();
        Map<Long, UserVO> collect = userVOS.stream().collect(Collectors.toMap(UserVO::getId, u -> u));
        //转换为VO
        List<AdoptionApplyVO> voList = new ArrayList<>(records.size());
        for (AdoptionApplication record : records) {
            AdoptionApplyVO vo = BeanUtil.copyProperties(record, AdoptionApplyVO.class);
            UserVO userVO = collect.get(record.getPublisherId());
            vo.setPublisherNickname(userVO.getNickname());
            vo.setPublisherAvatar(userVO.getAvatar());
            voList.add(vo);
        }
        pageDTO.setList(voList);
        return R.ok(pageDTO);
    }

    @Override
    @Transactional
    public R<String> processApplication(ProcessApplicationDTO processDTO) {
        //查询领养申请
        Long applicationId = processDTO.getApplicationId();
        AdoptionApplication application = getById(applicationId);
        //判断是否存在
        if (application == null) {
            throw new BadException("领养申请不存在");
        }
        //查询宠物表
        Long petId = application.getPetId();
        Pet pet = petService.getById(petId);
        if (pet == null) {
            throw new BadException("申请领养的宠物不存在");
        }
        //判断宠物状态是否为待领养
        if (pet.getStatus() != AdoptionStatus.AVAILABLE) {
            throw new BadException("宠物状态不是待领养");
        }
        //判断当前用户是否有权限处理
        Long userId = UserContext.getUserId();
        if (!userId.equals(application.getPublisherId())) {
            throw new BadException("您没有权限处理该领养申请");
        }
        //判断领养申请状态是否为待审核
        if (application.getStatus() != ApplicaltionStatus.PENDING) {
            throw new BadException("领养申请状态不是待审核");
        }
        //判断领养申请状态是否为已同意或已拒绝
        if (processDTO.getStatus() != ApplicaltionStatus.APPROVED && processDTO.getStatus() != ApplicaltionStatus.REJECTED) {
            throw new BadException("领养申请状态错误");
        }
        boolean isAccept = processDTO.getStatus() == ApplicaltionStatus.APPROVED;
        //先更新宠物状态，更新宠物状态的时候加乐观锁判断是否成功更新
        boolean update = petService.lambdaUpdate()
                .eq(Pet::getStatus, AdoptionStatus.AVAILABLE)
                .eq(Pet::getId, petId)
                .set(isAccept ,Pet::getStatus, AdoptionStatus.ADOPTED)
                .update();
        if (!update) throw new BadException("宠物状态更新失败");
        //更新领养申请状态
        application.setStatus(isAccept ?
                ApplicaltionStatus.APPROVED : ApplicaltionStatus.REJECTED);
        application.setAuditTime(LocalDateTime.now());
        application.setReason(isAccept ? null : processDTO.getRejectReason());
        updateById(application);
        //创建消息内容
        ProcessDTO dto = new ProcessDTO();
        dto.setPetId(petId);
        dto.setApplicationId(applicationId);
        dto.setApplicantId(Collections.singletonList(application.getApplicantId()));
        dto.setPublisherId(application.getPublisherId());
        dto.setIsAccept(isAccept);
        dto.setRejectReason(processDTO.getRejectReason());
        String messageId = applicationId + "_" + System.currentTimeMillis();
        //保存到本地补偿表
        MessageCompensation compensation = new MessageCompensation();
        compensation.setBusinessType(BusinessType.ADOPTION_APPROVE.getDesc());
        compensation.setApplicationId(applicationId);
        compensation.setReceiverIds(String.valueOf(application.getApplicantId()));
        compensation.setExchangeName(MqConstants.Exchange.PET_ADPTION_EXCHANGE);
        compensation.setRoutingKey(isAccept ?
                MqConstants.Key.PET_ADPTION_ACCEPT_ROUTING_KEY :
                MqConstants.Key.PET_ADPTION_REJECT_ROUTING_KEY);
        compensation.setMessageId(messageId);
        compensation.setMessageContent(JSONUtil.toJsonStr(dto));
        Long compensationId = messageCompensationService.savePendingMessage(compensation);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (isAccept) {
                    handleAccept(dto, compensationId);
                } else {
                    messageCompensationService.sendAndUpdateStatus(compensationId);
                }
            }
        });
        return R.ok("处理成功");
    }

    private void handleAccept(ProcessDTO dto, Long compensationId) {
        messageCompensationService.sendAndUpdateStatus(compensationId);
        List<AdoptionApplication> otherApps = lambdaQuery()
                .eq(AdoptionApplication::getPetId, dto.getPetId())
                .eq(AdoptionApplication::getPublisherId, dto.getPublisherId())
                .ne(AdoptionApplication::getApplicantId, dto.getApplicantId().get(0))
                .list();
        if (CollUtil.isEmpty(otherApps)) {
            return;
        }
        for (AdoptionApplication app : otherApps) {
            ProcessDTO rejectDTO = new ProcessDTO();
            rejectDTO.setPetId(dto.getPetId());
            rejectDTO.setApplicationId(app.getId());
            rejectDTO.setApplicantId(Collections.singletonList(app.getApplicantId()));
            rejectDTO.setPublisherId(dto.getPublisherId());
            rejectDTO.setIsAccept(false);
            rejectDTO.setRejectReason("该宠物已被其他人领养");

            String rejectMsgId = app.getId() + "_reject_" + System.currentTimeMillis();
            MessageCompensation compensation = new MessageCompensation();
            compensation.setBusinessType(BusinessType.ADOPTION_APPROVE.getDesc());
            compensation.setApplicationId(app.getId());
            compensation.setReceiverIds(String.valueOf(app.getApplicantId()));
            compensation.setExchangeName(MqConstants.Exchange.PET_ADPTION_EXCHANGE);
            compensation.setRoutingKey(MqConstants.Key.PET_ADPTION_REJECT_ROUTING_KEY);
            compensation.setMessageId(rejectMsgId);
            compensation.setMessageContent(JSONUtil.toJsonStr(rejectDTO));
            Long rejectCompId = messageCompensationService.savePendingMessage(compensation);
            messageCompensationService.sendAndUpdateStatus(rejectCompId);
        }
    }
}