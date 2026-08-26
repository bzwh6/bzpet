package com.bzpet.petpet.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * MQ消息本地补偿表
 * </p>
 *
 * @author bzwh
 * @since 2026-08-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("message_compensation")
@Schema(description="MQ消息本地补偿表")
public class MessageCompensation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description="主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description="业务类型：如ADOPTION_APPROVE")
    private String businessType;

    @Schema(description="业务ID（如申请单ID）")
    private Long applicationId;

    @Schema(description="接收者ID集合")
    private String receiverIds;

    @Schema(description="交换机名称")
    private String exchangeName;

    @Schema(description="路由键")
    private String routingKey;

    @Schema(description="全局唯一消息ID（复合ID或UUID）")
    private String messageId;

    @Schema(description="消息体内容（JSON）")
    private String messageContent;

    @Schema(description="状态：0待发送，1发送成功，2发送失败待重试，3死亡（人工介入）")
    private Integer status;

    @Schema(description="已重试次数")
    private Integer retryCount;

    @Schema(description="最后一次失败原因")
    private String lastErrorMsg;

    @Schema(description="创建时间")
    private LocalDateTime createTime;

    @Schema(description="更新时间")
    private LocalDateTime updateTime;


}
