package com.bzpet.petpet.service.impl;

import com.bzpet.petpet.domain.po.Pet;
import com.bzpet.petpet.mapper.PetMapper;
import com.bzpet.petpet.service.IPetService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 宠物表 服务实现类
 * </p>
 *
 * @author bzwh
 * @since 2026-08-21
 */
@Service
public class PetServiceImpl extends ServiceImpl<PetMapper, Pet> implements IPetService {

}
