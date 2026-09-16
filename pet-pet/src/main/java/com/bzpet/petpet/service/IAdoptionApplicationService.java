package com.bzpet.petpet.service;

import com.bzpet.common.domain.R;
import com.bzpet.common.domain.dto.PageDTO;
import com.bzpet.petpet.domain.dto.AdoptionApplyDTO;
import com.bzpet.petpet.domain.dto.ProcessApplicationDTO;
import com.bzpet.petpet.domain.po.AdoptionApplication;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bzpet.petpet.domain.query.AdoptionApplyQueryDTO;
import com.bzpet.petpet.domain.vo.AdoptionApplyVO;
import jakarta.validation.Valid;

/**
 * <p>
 * 宠物领养申请表 服务类
 * </p>
 *
 * @author bzwh
 * @since 2026-08-23
 */
public interface IAdoptionApplicationService extends IService<AdoptionApplication> {

    R<String> applyAdoption(@Valid AdoptionApplyDTO applyDTO);

    R<PageDTO<AdoptionApplyVO>> myAdoptionList(AdoptionApplyQueryDTO queryDTO);

    R<PageDTO<AdoptionApplyVO>> receivedList(AdoptionApplyQueryDTO queryDTO);

    R<String> processApplication(@Valid ProcessApplicationDTO processDTO);
}
