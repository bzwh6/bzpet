package com.bzpet.petpet.controller;



import com.bzpet.common.domain.R;
import com.bzpet.common.domain.dto.PageDTO;
import com.bzpet.common.domain.query.PageQuery;
import com.bzpet.petpet.domain.dto.PetDTO;
import com.bzpet.petpet.domain.query.PetQueryDTO;
import com.bzpet.petpet.domain.vo.BreedVO;
import com.bzpet.petpet.domain.vo.PetQueryVO;
import com.bzpet.petpet.service.IPetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    public R<PageDTO<PetQueryVO>> petList(@ParameterObject PetQueryDTO petDTO) {
        return petService.petList(petDTO);
    }

    @Operation(summary="分页查询我发布的宠物")
    @GetMapping("/my")
    public R<PageDTO<PetQueryVO>> myPetList(@ParameterObject PetQueryDTO petDTO) {
        return petService.myPetList(petDTO);
    }

    @Operation(summary="分页查询我点赞的宠物")
    @GetMapping("/liked")
    public R<PageDTO<PetQueryVO>> likedPetList(@ParameterObject PageQuery query) {
        return petService.likedPetList(query);
    }
    @Operation(summary="宠物品种字典")
    @GetMapping("/breeds")
    public R<List<BreedVO>> breedList() {
        return petService.breedList();
    }

    @Operation(summary="查询宠物详情")
    @GetMapping("/detail/{id}")
    public R<PetQueryVO> petDetail(@PathVariable Long id) throws InterruptedException {
        return petService.petDetail(id);
    }

    @Operation(summary="新增宠物")
    @PostMapping("/add")
    public void addPet(@RequestBody PetDTO petDTO) {
        petService.addPet(petDTO);
    }
    @Operation(summary="更新宠物")
    @PostMapping("/update/{id}")
    public void updatePet(@PathVariable Long id, @RequestBody PetDTO petDTO) {
        petService.updatePet(id, petDTO);
    }
    @Operation(summary="删除宠物")
    @DeleteMapping("/delete/{id}")
    public void deletePet(@PathVariable Long id) {
        petService.deletePet(id);
    }

    @Operation(summary = "点赞宠物接口")
    @PostMapping("/like/{id}")
    public void likePet(@PathVariable Long id) {
        petService.likePet(id);
    }

    @Operation(summary = "查询当前用户是否已点赞")
    @GetMapping("/like/status/{id}")
    public R<Boolean> isLiked(@PathVariable Long id) {
        return R.ok(petService.isLiked(id));
    }


}
