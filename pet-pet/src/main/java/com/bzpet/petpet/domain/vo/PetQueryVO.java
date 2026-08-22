package com.bzpet.petpet.domain.vo;

import com.bzpet.petpet.enums.AdoptionStatus;
import com.bzpet.petpet.enums.PetGender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PetQueryVO {
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

    @Schema(description="创建时间")
    private LocalDateTime createTime;

    @Schema(description="更新时间")
    private LocalDateTime updateTime;
}
