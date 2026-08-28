package com.bzpet.petpet.domain.query;

import com.bzpet.common.domain.query.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "通知查询参数")
public class NotificationQueryDTO extends PageQuery {
    @Schema(description = "通知类型")
    private Integer type;
    @Schema(description = "是否已读状态")
    private Integer isRead;
}
