package com.bzpet.petpet.domain.query;

import com.bzpet.common.domain.query.PageQuery;
import com.bzpet.petpet.enums.ApplicaltionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "分页查询宠物列表")
public class AdoptionApplyQueryDTO extends PageQuery {
    @Schema(description = "申请状态")
    private ApplicaltionStatus status;
}
