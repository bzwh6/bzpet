package com.bzpet.petpet.domain.query;

import com.bzpet.common.domain.query.PageQuery;
import com.bzpet.petpet.enums.AdoptionStatus;
import com.bzpet.petpet.enums.PetGender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "分页查询宠物列表")
public class PetQueryDTO extends PageQuery {
    @Schema(description = "宠物年龄")
    private Integer age;
    @Schema(description = "宠物性别")
    private Integer gender;
    @Schema(description = "领养状态")
    private Integer adoptionStatus;
    //TODO 品种
}
