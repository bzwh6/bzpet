package com.bzpet.petpet.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import com.bzpet.petpet.enums.ApplicaltionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 宠物领养申请表
 * </p>
 *
 * @author bzwh
 * @since 2026-08-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("adoption_application")
@Schema(name="AdoptionApplication对象", description="宠物领养申请表")
public class AdoptionApplication implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description="主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description="宠物ID")
    private Long petId;

    @Schema(description="申请人ID")
    private Long applicantId;

    @Schema(description="宠物发布人ID")
    private Long publisherId;

    @Schema(description="领养理由")
    private String reason;

    @Schema(description="状态: 0待审核 1已同意 2已拒绝")
    private ApplicaltionStatus status;

    @Schema(description="申请时间")
    private LocalDateTime createTime;

    @Schema(description="审核时间")
    private LocalDateTime auditTime;


}
