package com.bzpet.petpet.controller;



import com.bzpet.common.domain.R;
import com.bzpet.common.domain.dto.PageDTO;
import com.bzpet.petpet.domain.query.PetQueryDTO;
import com.bzpet.petpet.domain.vo.PetQueryVO;
import com.bzpet.petpet.service.IPetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 宠物表 前端控制器
 * </p>
 *
 * @author bzwh
 * @since 2026-08-21
 */
@RestController
@RequestMapping("/pets")
@Tag(name="宠物微服务接口")
public class PetController {
    @Autowired
    private IPetService petService;

    @Operation(summary="分页查询宠物列表")
    @GetMapping("/list")
    public R<PageDTO<PetQueryVO>> petList(PetQueryDTO petDTO) {
        return petService.petList(petDTO);
    }


}
