package com.bzpet.petpet.domain.vo;

import com.bzpet.common.enums.NotificationType;
import com.bzpet.common.enums.ReadStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {
    @Schema(description="业务Id")
    private Long applicationId;
    @Schema(description="通知类型")
    private NotificationType type;
    @Schema(description="标题")
    private String title;
    @Schema(description="通知内容")
    private String content;
    @Schema(description="已读状态")
    private ReadStatus isRead;
    @Schema(description="创建时间")
    private LocalDateTime createTime;
}
