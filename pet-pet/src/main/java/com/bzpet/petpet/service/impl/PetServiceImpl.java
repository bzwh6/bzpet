package com.bzpet.petpet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bzpet.common.domain.R;
import com.bzpet.common.domain.dto.PageDTO;
import com.bzpet.common.utils.UserContext;
import com.bzpet.petpet.domain.po.Pet;
import com.bzpet.petpet.domain.query.PetQueryDTO;
import com.bzpet.petpet.domain.vo.PetQueryVO;
import com.bzpet.petpet.mapper.PetMapper;
import com.bzpet.petpet.service.IPetService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 宠物表 服务实现类
 * </p>
 *
 * @author bzwh
 * @since 2026-08-21
 */
@Service
@Slf4j
public class PetServiceImpl extends ServiceImpl<PetMapper, Pet> implements IPetService {

    @Override
    public R<PageDTO<PetQueryVO>> petList(PetQueryDTO petDTO) {
        //分页查询宠物列表
        //获取分页查询参数
        Integer pageNo = petDTO.getPageNo();
        Integer pageSize = petDTO.getPageSize();
        //获取查询条件
        Integer age = petDTO.getAge();
        Integer gender = petDTO.getGender();
        Integer adoptionStatus = petDTO.getAdoptionStatus();
        //TODO 品种
        //分页查询宠物列表
        Page<Pet> page = lambdaQuery()
                .eq(age != null, Pet::getAge, age)
                .eq(gender != null, Pet::getGender, gender)
                .eq(adoptionStatus != null, Pet::getStatus, adoptionStatus)
                .orderByDesc(Pet::getCreateTime)
                .page(new Page<>(pageNo, pageSize));
        List<Pet> records = page.getRecords();

        PageDTO<PetQueryVO> result = new PageDTO<>();
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        if (CollUtil.isEmpty(records)){
            return R.ok(result);
        }
        // 5. 转换结果（Pet -> PetQueryVO）
        List<PetQueryVO> voList = records.stream()
                .map(pet -> BeanUtil.copyProperties(pet, PetQueryVO.class))
                .collect(Collectors.toList());
        // 6. 封装分页结果
        result.setList(voList);
        return R.ok(result);
    }
}
