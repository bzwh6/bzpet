package com.bzpet.petuser.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户信息DTO")
public class UserInfoDTO {

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;
}
