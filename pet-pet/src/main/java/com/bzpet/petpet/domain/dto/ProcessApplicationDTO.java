package com.bzpet.petpet.domain.dto;

import com.bzpet.petpet.enums.ApplicaltionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProcessApplicationDTO {
    @Schema(description = "领养申请ID")
    @NotNull(message = "领养申请ID不能为空")
    private Long applicationId;
    @Schema(description = "处理结果")
    @NotNull(message = "处理结果不能为空")
    private ApplicaltionStatus status;
    @Schema(description = "拒绝原因")
    private String rejectReason;
}

