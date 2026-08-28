package com.bzpet.petuser.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UserInfoVO {
    @Schema(description = "用户名")
    private String username;

    @Schema(description = "加密密码")
    private String password;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "状态 0正常 1禁用")
    private Boolean status;
}
