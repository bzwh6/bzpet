package com.bzpet.petpet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bzpet.common.constants.MqConstants;
import com.bzpet.common.domain.R;
import com.bzpet.common.domain.dto.PageDTO;
import com.bzpet.common.enums.NotificationType;
import com.bzpet.common.exception.BadException;
import com.bzpet.common.utils.UserContext;
import com.bzpet.petpet.domain.dto.AdoptionApplyDTO;
import com.bzpet.petpet.domain.dto.NotificationDTO;
import com.bzpet.petpet.domain.dto.ProcessApplicationDTO;
import com.bzpet.petpet.domain.po.AdoptionApplication;
import com.bzpet.petpet.domain.po.Pet;
import com.bzpet.petpet.domain.query.AdoptionApplyQueryDTO;
import com.bzpet.petpet.domain.vo.AdoptionApplyVO;
import com.bzpet.petpet.enums.AdoptionStatus;
import com.bzpet.petpet.enums.ApplicaltionStatus;
import com.bzpet.petpet.mapper.AdoptionApplicationMapper;
import com.bzpet.petpet.service.IAdoptionApplicationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bzpet.petpet.service.IPetService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        notificationDTO.setReceiverIds(Collections.singletonList(pet.getPublisherId()));
        notificationDTO.setSenderId(userId);
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
        if (CollUtil.isEmpty(records)){
            return R.ok(pageDTO);
        }
        //转换为VO
        List<AdoptionApplyVO> voList = new ArrayList<>(records.size());
        for (AdoptionApplication record : records) {
            AdoptionApplyVO vo = BeanUtil.copyProperties(record, AdoptionApplyVO.class);
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
        //更新领养申请状态
        boolean isAccept = processDTO.getStatus() == ApplicaltionStatus.APPROVED;
        application.setStatus(isAccept ?
                ApplicaltionStatus.APPROVED : ApplicaltionStatus.REJECTED);
        application.setAuditTime(LocalDateTime.now());
        updateById(application);
        //更新宠物状态
        pet.setStatus(isAccept ?
                AdoptionStatus.ADOPTED : AdoptionStatus.AVAILABLE);
        petService.updateById(pet);
        //利用同步回调，在事务提交后利用mq通知
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                //发送mq通知
                if (isAccept) {
                    handleAccept(application.getApplicantId(), petId);
                } else {
                    handleReject(application.getApplicantId(), petId);
                }
            }
        });
        return R.ok("处理成功");
    }
    private void handleAccept(Long applicanterId, Long petId) {
        NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setPetId(petId);
        notificationDTO.setReceiverIds(Collections.singletonList(applicanterId));
        rabbitTemplate.convertAndSend(
                MqConstants.Exchange.PET_ADPTION_EXCHANGE,
                MqConstants.Key.PET_ADPTION_ACCEPT_ROUTING_KEY,
                notificationDTO
        );
        rabbitTemplate.convertAndSend(
                MqConstants.Exchange.PET_ADPTION_EXCHANGE,
                MqConstants.Key.PET_ADPTION_REJECT_ROUTING_KEY,
                notificationDTO
        );
    }
    private void handleReject(Long applicanterId, Long petId) {
        NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setPetId(petId);
        notificationDTO.setReceiverIds(Collections.singletonList(applicanterId));
        rabbitTemplate.convertAndSend(
                MqConstants.Exchange.PET_ADPTION_EXCHANGE,
                MqConstants.Key.PET_ADPTION_REJECT_ROUTING_KEY,
                notificationDTO
        );
    }
}
