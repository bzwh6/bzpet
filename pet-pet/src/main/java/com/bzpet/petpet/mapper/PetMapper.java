package com.bzpet.petpet.mapper;

import com.bzpet.petpet.domain.po.Pet;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 宠物表 Mapper 接口
 * </p>
 *
 * @author bzwh
 * @since 2026-08-21
 */
@Mapper
public interface PetMapper extends BaseMapper<Pet> {

}
