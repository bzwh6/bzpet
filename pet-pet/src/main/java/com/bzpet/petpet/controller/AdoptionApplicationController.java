package com.bzpet.petpet.controller;


import com.bzpet.common.domain.R;
import com.bzpet.common.domain.dto.PageDTO;
import com.bzpet.petpet.domain.dto.AdoptionApplyDTO;
import com.bzpet.petpet.domain.dto.ProcessApplicationDTO;
import com.bzpet.petpet.domain.po.AdoptionApplication;
import com.bzpet.petpet.domain.query.AdoptionApplyQueryDTO;
import com.bzpet.petpet.domain.vo.AdoptionApplyVO;
import com.bzpet.petpet.service.IAdoptionApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 宠物领养申请表 前端控制器
 * </p>
 *
 * @author bzwh
 * @since 2026-08-23
 */
@RestController
@RequestMapping("/pets/adoption")
@Tag(name = "宠物领养申请相关接口")
public class AdoptionApplicationController {
    @Autowired
    private IAdoptionApplicationService applicationService;

    @PostMapping("/apply")
    @Operation(summary = "提交领养申请")
    public R<String> applyAdoption(@Valid @RequestBody AdoptionApplyDTO applyDTO) {
        return applicationService.applyAdoption(applyDTO);
    }

    @GetMapping("/my/list")
    @Operation(summary = "分页查询我的领养申请列表")
    public R<PageDTO<AdoptionApplyVO>> myAdoptionList(@ParameterObject AdoptionApplyQueryDTO pageDTO) {
        return applicationService.myAdoptionList(pageDTO);
    }

    @GetMapping("/received/list")
    @Operation(summary = "分页查询我收到的领养申请列表（发布人）")
    public R<PageDTO<AdoptionApplyVO>> receivedList(@ParameterObject AdoptionApplyQueryDTO pageDTO) {
        return applicationService.receivedList(pageDTO);
    }
    @PostMapping("/process")
    @Operation(summary = "处理领养申请")
    public R<String> processApplication(@Valid @RequestBody ProcessApplicationDTO processDTO) {
        return applicationService.processApplication(processDTO);
    }
}
