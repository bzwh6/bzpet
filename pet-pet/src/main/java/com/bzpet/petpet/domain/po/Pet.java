package com.bzpet.petpet.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import com.bzpet.petpet.enums.AdoptionStatus;
import com.bzpet.petpet.enums.PetGender;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 宠物表
 * </p>
 *
 * @author bzwh
 * @since 2026-08-21
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pet")
@Schema(name="Pet对象", description="宠物表")
public class Pet implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description="主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description="宠物昵称")
    private String petName;

    @Schema(description="品种")
    private String breed;

    @Schema(description="年龄")
    private Integer age;

    @Schema(description="性别 0公 1母")
    private PetGender gender;

    @Schema(description="性格描述")
    private String description;

    @Schema(description="图片链接")
    private String imageUrl;

    @Schema(description="发布人ID")
    private Long publisherId;

    @Schema(description="状态 0待领养 1已领养 2下架")
    private AdoptionStatus status;

    @Schema(description="点赞数量")
    private Integer likesCount;

    @Schema(description="创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description="更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;


}
