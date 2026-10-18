package com.bzpet.petpet.domain.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.bzpet.common.enums.NotificationType;
import com.bzpet.common.enums.ReadStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationQueryVO {
    @Schema(description="主键ID")
    private Long id;

    @Schema(description="发布人ID（接收者）")
    private Long publisherId;

    @Schema(description="申请人ID（触发者）")
    private Long applicantId;

    @Schema(description="宠物ID（方便点击跳转详情）")
    private Long petId;

    @Schema(description = "对应业务Id（跳转目标）")
    private Long applicationId;

    @Schema(description = "幂等键：接收者:业务id:类型")
    private String idemKey;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description="标题（即申请人的申请内容）")
    private String title;

    @Schema(description="通知内容（固定的业务模板）")
    private String content;

    @Schema(description="通知类型：0领养申请 1审核通过 2系统通知")
    private NotificationType type;

    @Schema(description="已读状态：0未读 1已读")
    private ReadStatus isRead;
}
