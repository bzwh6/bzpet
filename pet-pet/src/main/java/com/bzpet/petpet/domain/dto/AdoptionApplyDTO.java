package com.bzpet.petpet.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description="宠物领养申请DTO")
public class AdoptionApplyDTO {
    @Schema(description="宠物ID")
    @NotNull(message = "宠物ID不能为空")
    private Long petId;
    @Schema(description="领养理由")
    @Size(max = 255, message = "领养理由最多255个字符")
    private String reason;
}
