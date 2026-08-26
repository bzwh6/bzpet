package com.bzpet.petpet.service.impl;

import com.bzpet.petpet.domain.po.MessageCompensation;
import com.bzpet.petpet.mapper.MessageCompensationMapper;
import com.bzpet.petpet.service.IMessageCompensationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * MQ消息本地补偿表 服务实现类
 * </p>
 *
 * @author bzwh
 * @since 2026-08-26
 */
@Service
public class MessageCompensationServiceImpl extends ServiceImpl<MessageCompensationMapper, MessageCompensation> implements IMessageCompensationService {

}
