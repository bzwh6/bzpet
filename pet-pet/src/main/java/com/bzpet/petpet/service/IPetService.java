package com.bzpet.petpet.service;

import com.bzpet.common.domain.dto.PageDTO;
import com.bzpet.common.domain.R;
import com.bzpet.petpet.domain.dto.PetDTO;
import com.bzpet.petpet.domain.po.Pet;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bzpet.petpet.domain.query.PetQueryDTO;
import com.bzpet.petpet.domain.vo.BreedVO;
import com.bzpet.petpet.domain.vo.PetQueryVO;

import java.util.List;

/**
 * <p>
 * 宠物表 服务类
 * </p>
 *
 * @author bzwh
 * @since 2026-08-21
 */
public interface IPetService extends IService<Pet> {

    R<PageDTO<PetQueryVO>> petList(PetQueryDTO petDTO);

    R<PetQueryVO> petDetail(Long id) throws InterruptedException;

    /**
     * 宠物品种字典（前端下拉与展示映射用）
     */
    R<List<BreedVO>> breedList();

    void addPet(PetDTO petDTO);

    void deletePet(Long id);

    void updatePet(Long id, PetDTO petDTO);

    void likePet(Long id);

    /**
     * 当前用户是否已点赞该宠物
     */
    boolean isLiked(Long petId);

    void saveLikeCount();
}
