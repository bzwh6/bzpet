package com.bzpet.petpet.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import com.bzpet.common.enums.NotificationType;
import com.bzpet.common.enums.ReadStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 站内通知表
 * </p>
 *
 * @author bzwh
 * @since 2026-08-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("notification")
@Schema(name="Notification对象", description="站内通知表")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description="主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description="发布人ID（接收者）")
    private Long publisherId;

    @Schema(description="申请人ID（触发者）")
    private Long applicantId;

    @Schema(description="宠物ID（方便点击跳转详情）")
    private Long petId;

    @Schema(description = "对应业务Id")
    private Long applicationId;

    @Schema(description = "接收者ID（通知发给谁）")
    private Long receiverId;

    @Schema(description="标题（即申请人的申请内容）")
    private String title;

    @Schema(description="通知内容（固定的业务模板）")
    private String content;

    @Schema(description="通知类型：0领养申请 1审核通过 2系统通知")
    private NotificationType type;

    @Schema(description="已读状态：0未读 1已读")
    private ReadStatus isRead;

    @Schema(description="创建时间")
    private LocalDateTime createTime;


}
