package com.bzpet.petpet.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description="处理申请DTO")
@Data
public class ProcessDTO {
    @Schema(description="宠物ID")
    private Long petId;
    @Schema(description="申请ID")
    private Long applicationId;
    @Schema(description="申请人ID")
    private List<Long> applicantId;
    @Schema(description="发布人ID")
    private Long publisherId;
    @Schema(description = "拒绝原因")
    private String rejectReason;
    @Schema(description="是否同意")
    private Boolean isAccept;
}