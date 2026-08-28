package com.bzpet.petapi.client.user;

import com.bzpet.common.domain.R;
import com.bzpet.petapi.domain.vo.UserInfoVO;
import com.bzpet.petapi.domain.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "pet-user")
public interface UserClient {
    @GetMapping("/user/userInfo/{id}")
    @Operation(summary = "用户信息接口")
    public R<UserInfoVO> userInfo(@PathVariable Long id);

    @GetMapping("/list")
    @Operation(summary = "用户列表接口")
    public R<List<UserVO>> userInfoList(@RequestParam List<Long> ids);
}
