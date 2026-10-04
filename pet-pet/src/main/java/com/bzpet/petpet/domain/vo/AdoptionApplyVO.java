package com.bzpet.petpet.domain.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.bzpet.petpet.enums.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>
 * 宠物领养申请表
 * </p>
 *
 * @author bzwh
 * @since 2026-08-23
 */
@Data
public class AdoptionApplyVO  {
    @Schema(description="主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description="宠物ID")
    private Long petId;

    @Schema(description="申请人ID")
    private Long applicantId;

    @Schema(description="宠物发布人ID")
    private Long publisherId;

    @Schema(description = "发布人呢称")
    private String publisherNickname;

    @Schema(description = "发布人头像")
    private String publisherAvatar;

    @Schema(description = "申请人人称")
    private String applicantNickname;

    @Schema(description = "申请人头像")
    private String applicantAvatar;

    @Schema(description="领养理由")
    private String reason;

    @Schema(description="状态: 0待审核 1已同意 2已拒绝")
    private ApplicationStatus status;

    @Schema(description="申请时间")
    private LocalDateTime createTime;

    @Schema(description="审核时间")
    private LocalDateTime auditTime;


}
