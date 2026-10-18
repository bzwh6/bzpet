package com.bzpet.petpet.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 宠物品种字典项，供前端下拉与展示映射使用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "宠物品种字典项")
public class BreedVO {

    @Schema(description = "品种编码")
    private Integer code;

    @Schema(description = "品种名称")
    private String name;
}
